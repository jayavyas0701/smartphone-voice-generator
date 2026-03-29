package com.hackathon.voicenavigator.data.api

import android.content.Context
import android.util.Log
import com.hackathon.voicenavigator.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * RAG (Retrieval Augmented Generation) Engine
 *
 * Pipeline:
 *   Text → Chunk → Embed (local TF-IDF OR Gemini) → Vector Store
 *   Query → Embed → Cosine Similarity → Top-K Chunks → Gemini LLM → Response
 *
 * EMBEDDING STRATEGY:
 *   Primary:  Gemini text-embedding-004 (when API key is valid)
 *   Fallback: Local TF-IDF sparse vectors (zero API calls, always works)
 *
 * The fallback means RAG works even if the Gemini embedding API key is
 * missing or returns 403. Quality is slightly lower than neural embeddings
 * but retrieval is still accurate for keyword-rich DMV/ESG content.
 */
class RAGEngine(private val context: Context) {

    companion object {
        private const val TAG = "RAGEngine"
        private const val CHUNK_SIZE = 500
        private const val CHUNK_OVERLAP = 50
        private const val TOP_K = 5
        // Vocabulary size for local TF-IDF vectors
        private const val VOCAB_SIZE = 512
    }

    // ── Vector Store ──────────────────────────────────────────
    private val vectorStores = mutableMapOf<String, List<EmbeddedChunk>>()
    private val _initialized = mutableMapOf<String, Boolean>()

    data class EmbeddedChunk(
        val text: String,
        val embedding: List<Double>,
        val source: String,
        val chunkIndex: Int
    )

    fun isInitialized(source: String): Boolean = _initialized[source] == true
    fun getChunkCount(source: String): Int = vectorStores[source]?.size ?: 0

    // ================================================================
    // STEP 1: Load Text
    // ================================================================

    private suspend fun extractTextFromAsset(assetFileName: String): String = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.assets.open(assetFileName)
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val text = stripper.getText(document)
            document.close(); inputStream.close()
            Log.d(TAG, "Extracted ${text.length} chars from $assetFileName")
            text
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction failed: ${e.message}")
            try { context.assets.open(assetFileName).bufferedReader().readText() }
            catch (e2: Exception) { Log.e(TAG, "Text fallback also failed: ${e2.message}"); "" }
        }
    }

    // ================================================================
    // STEP 2: Chunk Text
    // ================================================================

    fun chunkText(text: String, chunkSize: Int = CHUNK_SIZE, overlap: Int = CHUNK_OVERLAP): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        if (words.size <= chunkSize) return listOf(words.joinToString(" "))
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            val end = minOf(start + chunkSize, words.size)
            chunks.add(words.subList(start, end).joinToString(" "))
            start += (chunkSize - overlap)
        }
        Log.d(TAG, "Created ${chunks.size} chunks from ${words.size} words")
        return chunks
    }

    // ================================================================
    // STEP 3a: Local TF-IDF Embedding (no API, always works)
    // ================================================================

    /**
     * Build a corpus-level vocabulary from all chunks.
     * Selects the top VOCAB_SIZE terms by document frequency.
     */
    private fun buildVocabulary(chunks: List<String>): List<String> {
        val dfCounts = mutableMapOf<String, Int>()
        chunks.forEach { chunk ->
            tokenize(chunk).toSet().forEach { term ->
                dfCounts[term] = (dfCounts[term] ?: 0) + 1
            }
        }
        // Pick terms that appear in at least 2 docs but not in all (informative terms)
        return dfCounts.entries
            .filter { it.value in 2 until chunks.size }
            .sortedByDescending { it.value }
            .take(VOCAB_SIZE)
            .map { it.key }
            .ifEmpty {
                // Fallback: just take most frequent terms if corpus is tiny
                dfCounts.entries.sortedByDescending { it.value }.take(VOCAB_SIZE).map { it.key }
            }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 }  // skip very short tokens
    }

    /**
     * Compute TF-IDF vector for a single text against the corpus vocabulary.
     * Returns a dense vector of length VOCAB_SIZE.
     */
    private fun tfidfVector(text: String, vocabulary: List<String>, idfScores: Map<String, Double>): List<Double> {
        val tokens = tokenize(text)
        val totalTokens = tokens.size.coerceAtLeast(1)
        val tfCounts = tokens.groupingBy { it }.eachCount()

        val vector = vocabulary.map { term ->
            val tf = (tfCounts[term] ?: 0).toDouble() / totalTokens
            val idf = idfScores[term] ?: 0.0
            tf * idf
        }

        // L2 normalize
        val norm = sqrt(vector.sumOf { it * it }).coerceAtLeast(1e-10)
        return vector.map { it / norm }
    }

    private fun computeIdf(vocabulary: List<String>, chunks: List<String>): Map<String, Double> {
        val n = chunks.size.toDouble()
        return vocabulary.associateWith { term ->
            val df = chunks.count { tokenize(it).contains(term) }.coerceAtLeast(1)
            ln(n / df) + 1.0
        }
    }

    // ================================================================
    // STEP 3b: Gemini Neural Embedding (better quality, needs valid API key)
    // ================================================================

    private suspend fun tryGeminiEmbedding(text: String): List<Double>? {
        return try {
            val result = GeminiApiService.generateEmbedding(text)
            if (result == null) Log.w(TAG, "Gemini embedding returned null")
            result
        } catch (e: Exception) {
            Log.w(TAG, "Gemini embedding threw: ${e.message}")
            null
        }
    }

    // ================================================================
    // STEP 4: Cosine Similarity Search
    // ================================================================

    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0; var normA = 0.0; var normB = 0.0
        for (i in a.indices) { dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i] }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0.0 else dot / denom
    }

    private fun searchSimilar(queryEmbedding: List<Double>, source: String, topK: Int = TOP_K): List<Pair<EmbeddedChunk, Double>> {
        val store = vectorStores[source] ?: return emptyList()
        return store
            .map { chunk -> Pair(chunk, cosineSimilarity(queryEmbedding, chunk.embedding)) }
            .sortedByDescending { it.second }
            .take(topK)
    }

    // ================================================================
    // STEP 5: Initialize (build vector store)
    // ================================================================

    suspend fun initializeSource(sourceName: String, assetFileName: String, isPdf: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            if (_initialized[sourceName] == true) return@withContext true
            val text = if (isPdf) extractTextFromAsset(assetFileName) else
                try { context.assets.open(assetFileName).bufferedReader().readText() } catch (e: Exception) { "" }
            if (text.isBlank()) { Log.e(TAG, "No text from $assetFileName"); return@withContext false }
            buildVectorStore(sourceName, text)
        }

    suspend fun initializeFromText(sourceName: String, textContent: String): Boolean =
        withContext(Dispatchers.IO) {
            if (_initialized[sourceName] == true) return@withContext true
            if (textContent.isBlank()) { Log.e(TAG, "Empty text for $sourceName"); return@withContext false }
            Log.d(TAG, "Initializing '$sourceName' from inline text (${textContent.length} chars)")
            buildVectorStore(sourceName, textContent)
        }

    private suspend fun buildVectorStore(sourceName: String, text: String): Boolean {
        val chunks = chunkText(text)
        if (chunks.isEmpty()) { Log.e(TAG, "0 chunks produced for $sourceName"); return false }
        Log.d(TAG, "${chunks.size} chunks — trying Gemini embeddings first...")

        // Try Gemini on first chunk to see if API key is valid
        val testEmbedding = tryGeminiEmbedding(chunks[0])
        val useGemini = testEmbedding != null
        Log.d(TAG, if (useGemini) "✓ Gemini embeddings available" else "⚠ Gemini unavailable — using local TF-IDF embeddings")

        val embeddedChunks: List<EmbeddedChunk>

        if (useGemini) {
            // Full Gemini embeddings
            val embeddings = mutableListOf(testEmbedding!!)
            chunks.drop(1).forEachIndexed { i, chunk ->
                Log.d(TAG, "Gemini embedding ${i + 2}/${chunks.size}")
                embeddings.add(tryGeminiEmbedding(chunk) ?: run {
                    Log.w(TAG, "Chunk ${i+2} failed — using zero vector"); List(testEmbedding.size) { 0.0 }
                })
            }
            embeddedChunks = chunks.mapIndexed { i, chunk ->
                EmbeddedChunk(text = chunk, embedding = embeddings[i], source = sourceName, chunkIndex = i)
            }
        } else {
            // Local TF-IDF — zero API calls, fully offline
            val vocabulary = buildVocabulary(chunks)
            val idfScores = computeIdf(vocabulary, chunks)
            Log.d(TAG, "TF-IDF vocab size: ${vocabulary.size}")
            embeddedChunks = chunks.mapIndexed { i, chunk ->
                EmbeddedChunk(
                    text = chunk,
                    embedding = tfidfVector(chunk, vocabulary, idfScores),
                    source = sourceName,
                    chunkIndex = i
                )
            }
        }

        vectorStores[sourceName] = embeddedChunks
        _initialized[sourceName] = true
        val method = if (useGemini) "Gemini neural" else "local TF-IDF"
        Log.d(TAG, "✓ '$sourceName' initialized: ${embeddedChunks.size} chunks via $method embeddings")
        return true
    }

    // ================================================================
    // STEP 6: Query
    // ================================================================

    suspend fun query(
        userQuery: String,
        sourceName: String,
        systemPrompt: String,
        topK: Int = TOP_K
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized(sourceName))
                return@withContext Result.failure(Exception("Source '$sourceName' not initialized"))

            if (getChunkCount(sourceName) == 0)
                return@withContext Result.failure(Exception("Source '$sourceName' has 0 chunks"))

            // Embed the query with the same method used during indexing
            val store = vectorStores[sourceName]!!
            val embeddingDim = store.first().embedding.size

            val queryEmbedding: List<Double> = if (embeddingDim > VOCAB_SIZE) {
                // Gemini was used (768 dims) — use Gemini for query too
                tryGeminiEmbedding(userQuery)
                    ?: return@withContext Result.failure(Exception("Query embedding failed"))
            } else {
                // TF-IDF was used — reconstruct query vector against stored chunks
                val allChunks = store.map { it.text }
                val vocabulary = buildVocabulary(allChunks)
                val idfScores = computeIdf(vocabulary, allChunks)
                tfidfVector(userQuery, vocabulary, idfScores)
            }

            val results = searchSimilar(queryEmbedding, sourceName, topK)
            Log.d(TAG, "Retrieved ${results.size} chunks (top score: ${"%.3f".format(results.firstOrNull()?.second ?: 0.0)})")

            val retrievedContext = results.joinToString("\n\n---\n\n") { (chunk, score) ->
                "[Relevance: ${"%.3f".format(score)}]\n${chunk.text}"
            }

            val messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", """
Based on the following retrieved document passages:

$retrievedContext

---

User Question: $userQuery

Provide a detailed answer based STRICTLY on the document passages above.
Include specific data, statistics, and facts from the passages.
If the answer is not found in the passages, say so clearly.
                """.trimIndent())
            )

            GeminiApiService.chatCompletion(messages, maxTokens = 1024)
        } catch (e: Exception) {
            Log.e(TAG, "RAG query failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun clearSource(sourceName: String) {
        vectorStores.remove(sourceName)
        _initialized.remove(sourceName)
    }
}