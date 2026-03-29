package com.hackathon.voicenavigator.data.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hackathon.voicenavigator.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * RAG (Retrieval Augmented Generation) Engine
 *
 * Implements the full RAG pipeline as per the architecture:
 *   Data Sources (PDF) → Transform into Embeddings (NLP) → IR Search → LLM → Response
 *
 * Steps:
 *   1. Load PDF from assets → Extract text
 *   2. Chunk text into passages (~500 tokens each)
 *   3. Generate embeddings for each chunk via OpenAI Embeddings API
 *   4. On user query: embed the query → cosine similarity search → retrieve top-K chunks
 *   5. Send retrieved context + user query to LLM (ChatGPT) → return grounded response
 */
class RAGEngine(private val context: Context) {

    companion object {
        private const val TAG = "RAGEngine"
        private const val CHUNK_SIZE = 500       // ~500 words per chunk
        private const val CHUNK_OVERLAP = 50     // overlap between chunks for continuity
        private const val TOP_K = 5              // number of chunks to retrieve
        private const val EMBEDDING_MODEL = "embedding-001"  // Google Gemini free tier model
        private const val USE_GEMINI = true      // Use Gemini for embeddings
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // ── Vector Store (in-memory) ───────────────────────────────
    // Each document source has its own list of embedded chunks
    private val vectorStores = mutableMapOf<String, List<EmbeddedChunk>>()

    data class EmbeddedChunk(
        val text: String,
        val embedding: List<Double>,
        val source: String,
        val chunkIndex: Int
    )

    // ── Initialization Status ──────────────────────────────────
    private val _initialized = mutableMapOf<String, Boolean>()
    fun isInitialized(source: String): Boolean = _initialized[source] == true

    // ================================================================
    // STEP 1: Load & Extract Text from PDF Assets
    // ================================================================

    /**
     * Extract text from a PDF file in assets folder.
     * Uses Android's built-in PDF rendering or simple text extraction.
     */
    private suspend fun extractTextFromAsset(assetFileName: String): String = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.assets.open(assetFileName)
            // Use PdfBox for text extraction
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()
            Log.d(TAG, "Extracted ${text.length} chars from $assetFileName")
            text
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF text from $assetFileName: ${e.message}")
            // Fallback: try reading as plain text asset
            try {
                context.assets.open(assetFileName).bufferedReader().readText()
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback text read also failed: ${e2.message}")
                ""
            }
        }
    }

    /**
     * Extract text from a raw text asset (pre-extracted PDF content)
     */
    private suspend fun loadTextAsset(assetFileName: String): String = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetFileName).bufferedReader().readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load text asset $assetFileName: ${e.message}")
            ""
        }
    }

    // ================================================================
    // STEP 2: Chunk Text into Passages
    // ================================================================

    /**
     * Split text into overlapping chunks of approximately CHUNK_SIZE words.
     * Overlap ensures that context is not lost at chunk boundaries.
     */
    fun chunkText(text: String, chunkSize: Int = CHUNK_SIZE, overlap: Int = CHUNK_OVERLAP): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size <= chunkSize) return listOf(words.joinToString(" "))

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            val end = minOf(start + chunkSize, words.size)
            val chunk = words.subList(start, end).joinToString(" ")
            if (chunk.isNotBlank()) {
                chunks.add(chunk)
            }
            start += (chunkSize - overlap)
        }
        Log.d(TAG, "Created ${chunks.size} chunks from ${words.size} words")
        return chunks
    }

    // ================================================================
    // STEP 3: Generate Embeddings via Google Gemini API
    // ================================================================

    /**
     * Generate embedding vector for a single text using Google Gemini Embeddings API.
     * Model: text-embedding-004 (768 dimensions)
     */
    private suspend fun generateEmbedding(text: String): List<Double>? = withContext(Dispatchers.IO) {
        try {
            if (USE_GEMINI) {
                // Use Gemini embeddings
                GeminiApiService.generateEmbedding(text)
            } else {
                // Fallback to OpenAI (if needed)
                val requestBody = gson.toJson(
                    mapOf(
                        "input" to text.take(8000), // API limit
                        "model" to EMBEDDING_MODEL
                    )
                )

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/embeddings")
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Embedding API error: ${response.code} - $body")
                    return@withContext null
                }

                val json = JsonParser.parseString(body).asJsonObject
                val dataArray = json.getAsJsonArray("data")
                val embeddingArray = dataArray[0].asJsonObject.getAsJsonArray("embedding")
                embeddingArray.map { it.asDouble }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate embedding: ${e.message}")
            null
        }
    }

    /**
     * Generate embeddings for a batch of chunks.
     * Processes sequentially to avoid rate limits.
     */
    private suspend fun generateEmbeddings(chunks: List<String>): List<List<Double>?> {
        return chunks.mapIndexed { index, chunk ->
            Log.d(TAG, "Embedding chunk ${index + 1}/${chunks.size}")
            val embedding = generateEmbedding(chunk)
            embedding
        }
    }

    // ================================================================
    // STEP 4: Vector Store & Similarity Search
    // ================================================================

    /**
     * Compute cosine similarity between two vectors.
     */
    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size) return 0.0
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }

    /**
     * Search the vector store for the most similar chunks to the query.
     * Returns top-K chunks sorted by similarity score.
     */
    private fun searchSimilar(
        queryEmbedding: List<Double>,
        source: String,
        topK: Int = TOP_K
    ): List<Pair<EmbeddedChunk, Double>> {
        val store = vectorStores[source] ?: return emptyList()
        return store
            .map { chunk -> Pair(chunk, cosineSimilarity(queryEmbedding, chunk.embedding)) }
            .sortedByDescending { it.second }
            .take(topK)
    }

    // ================================================================
    // STEP 5: Full RAG Pipeline
    // ================================================================

    /**
     * Initialize the RAG pipeline for a specific document source.
     * This loads the PDF, chunks it, and generates embeddings.
     * Should be called once (e.g., on app start or first query).
     */
    suspend fun initializeSource(
        sourceName: String,
        assetFileName: String,
        isPdf: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        if (_initialized[sourceName] == true) {
            Log.d(TAG, "Source $sourceName already initialized")
            return@withContext true
        }

        Log.d(TAG, "Initializing RAG source: $sourceName from $assetFileName")

        // Step 1: Extract text
        val text = if (isPdf) extractTextFromAsset(assetFileName) else loadTextAsset(assetFileName)
        if (text.isBlank()) {
            Log.e(TAG, "No text extracted from $assetFileName")
            return@withContext false
        }

        // Step 2: Chunk
        val chunks = chunkText(text)
        Log.d(TAG, "Created ${chunks.size} chunks for $sourceName")

        // Step 3: Generate embeddings
        val embeddings = generateEmbeddings(chunks)

        // Step 4: Store in vector store
        val embeddedChunks = chunks.mapIndexedNotNull { index, chunk ->
            val embedding = embeddings[index]
            if (embedding != null) {
                EmbeddedChunk(
                    text = chunk,
                    embedding = embedding,
                    source = sourceName,
                    chunkIndex = index
                )
            } else null
        }

        vectorStores[sourceName] = embeddedChunks
        _initialized[sourceName] = true
        Log.d(TAG, "Initialized $sourceName with ${embeddedChunks.size} embedded chunks")
        true
    }

    /**
     * Initialize a RAG source from pre-provided text content (no PDF needed).
     * Useful when PDF extraction isn't available and you have the text already.
     */
    suspend fun initializeFromText(
        sourceName: String,
        textContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (_initialized[sourceName] == true) return@withContext true

        val chunks = chunkText(textContent)
        val embeddings = generateEmbeddings(chunks)

        val embeddedChunks = chunks.mapIndexedNotNull { index, chunk ->
            val embedding = embeddings[index]
            if (embedding != null) {
                EmbeddedChunk(text = chunk, embedding = embedding, source = sourceName, chunkIndex = index)
            } else null
        }

        vectorStores[sourceName] = embeddedChunks
        _initialized[sourceName] = true
        Log.d(TAG, "Initialized $sourceName from text with ${embeddedChunks.size} embedded chunks")
        true
    }

    /**
     * Execute a RAG query against a specific source.
     *
     * Pipeline:  Query → Embed → IR Search → Retrieve top-K → LLM (Prompt + Knowledge) → Response
     */
    suspend fun query(
        userQuery: String,
        sourceName: String,
        systemPrompt: String,
        topK: Int = TOP_K
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if source is initialized
            if (_initialized[sourceName] != true) {
                return@withContext Result.failure(
                    Exception("Source '$sourceName' not initialized. Call initializeSource() first.")
                )
            }

            // Step 1: Embed the user query
            val queryEmbedding = generateEmbedding(userQuery)
                ?: return@withContext Result.failure(Exception("Failed to embed query"))

            // Step 2: IR Search - find most relevant chunks
            val results = searchSimilar(queryEmbedding, sourceName, topK)
            Log.d(TAG, "Found ${results.size} relevant chunks (top similarity: ${results.firstOrNull()?.second})")

            // Step 3: Build context from retrieved chunks
            val retrievedContext = results.joinToString("\n\n---\n\n") { (chunk, score) ->
                "[Relevance: ${"%.3f".format(score)}]\n${chunk.text}"
            }

            // Step 4: Send to LLM with Prompt + Knowledge
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

            OpenAIApiService.chatCompletion(messages, maxTokens = 2048)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the number of chunks stored for a source
     */
    fun getChunkCount(sourceName: String): Int = vectorStores[sourceName]?.size ?: 0

    /**
     * Clear a specific source from the vector store
     */
    fun clearSource(sourceName: String) {
        vectorStores.remove(sourceName)
        _initialized.remove(sourceName)
    }

    /**
     * Get API key from OpenAIApiService
     */
    private fun getApiKey(): String {
        return OpenAIApiService.getApiKey()
    }
}
