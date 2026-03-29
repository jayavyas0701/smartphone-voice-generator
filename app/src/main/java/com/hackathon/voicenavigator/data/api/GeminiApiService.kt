package com.hackathon.voicenavigator.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hackathon.voicenavigator.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Google Gemini API Service
 * Used for embeddings (embedding-001 model - free tier compatible)
 */
object GeminiApiService {

    private const val TAG = "GeminiApiService"
    private const val EMBEDDING_MODEL = "models/gemini-embedding-001"  // Free tier model
    private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val LLM_MODEL = "gemini-2.0-flash"
    
    // Rate limiting for free tier (15 requests/min = ~4 sec per request)
    private const val MIN_REQUEST_INTERVAL_MS = 4000L
    private var lastRequestTime = 0L
    private var lastEmbedTime = 0L

    private var apiKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun setApiKey(key: String) {
        apiKey = key
    }

    fun getApiKey(): String = apiKey

    // ======================== RATE LIMITING ========================

    /**
     * Wait to maintain minimum interval between LLM requests
     * Free tier limit: ~15 requests/minute
     */
    private suspend fun waitForLLMRateLimit() {
        val now = System.currentTimeMillis()
        val timeSinceLastRequest = now - lastRequestTime
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            val waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            Log.d(TAG, "Rate limit: waiting ${waitTime}ms")
            delay(waitTime)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    private suspend fun waitForEmbeddingRateLimit() {
        val now = System.currentTimeMillis()
        val timeSinceLastEmbed = now - lastEmbedTime
        val minInterval = 2000L // Embeddings can be slightly faster
        if (timeSinceLastEmbed < minInterval) {
            val waitTime = minInterval - timeSinceLastEmbed
            delay(waitTime)
        }
        lastEmbedTime = System.currentTimeMillis()
    }

    // ======================== EMBEDDINGS ========================
    suspend fun generateEmbedding(text: String): List<Double>? = withContext(Dispatchers.IO) {
        try {
            waitForEmbeddingRateLimit()
            
            val requestBody = gson.toJson(
                mapOf(
                    "model" to EMBEDDING_MODEL,
                    "content" to mapOf(
                        "parts" to listOf(
                            mapOf(
                                "text" to text.take(20000) // Gemini API limit
                            )
                        )
                    )
                )
            )

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Embedding API error: ${response.code} - $body")
                return@withContext null
            }

            // Parse Gemini response: { "embedding": { "values": [0.123, ...] } }
            val json = JsonParser.parseString(body).asJsonObject
            val embeddingObj = json.getAsJsonObject("embedding")
            val valuesArray = embeddingObj.getAsJsonArray("values")
            
            Log.d(TAG, "✓ Generated embedding (length: ${valuesArray.size()})")
            valuesArray.map { it.asDouble }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate embedding: ${e.message}")
            null
        }
    }

    /**
     * Generate embeddings for batch of texts
     */
    suspend fun generateEmbeddings(texts: List<String>): List<List<Double>?> {
        return texts.mapIndexed { index, text ->
            Log.d(TAG, "Embedding text ${index + 1}/${texts.size}")
            generateEmbedding(text)
        }
    }

    // ======================== LLM / CHAT ========================

    /**
     * Chat completion with retry + backoff for rate limiting
     * Respects free tier limits (~15 req/min)
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String = LLM_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = 3
        
        while (retryCount < maxRetries) {
            try {
                waitForLLMRateLimit()
                
                // Reduce context to minimize tokens (free tier limit!)
                val trimmedMessages = messages.map { msg ->
                    msg.copy(content = msg.content.take(1000))
                }
                
                val contents = trimmedMessages.map { msg ->
                    mapOf(
                        "role" to if (msg.role == "user") "user" else "model",
                        "parts" to listOf(mapOf("text" to msg.content))
                    )
                }

                val requestBody = gson.toJson(
                    mapOf(
                        "contents" to contents,
                        "generationConfig" to mapOf(
                            "temperature" to temperature,
                            "maxOutputTokens" to minOf(maxTokens, 512)
                        )
                    )
                )

                val url = "$API_BASE/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                // Handle rate limit (429) with exponential backoff
                if (response.code == 429) {
                    val waitMs = (2000L * Math.pow(2.0, retryCount.toDouble())).toLong()
                    Log.w(TAG, "Rate limited. Retry ${retryCount + 1}/$maxRetries in ${waitMs}ms")
                    retryCount++
                    delay(waitMs)
                    continue
                }

                if (!response.isSuccessful) {
                    Log.e(TAG, "LLM error ${response.code}")
                    return@withContext Result.failure(Exception("Error ${response.code}"))
                }

                val json = JsonParser.parseString(body).asJsonObject
                val candidates = json.getAsJsonArray("candidates")
                if (candidates.size() == 0) {
                    return@withContext Result.failure(Exception("Empty response"))
                }

                val text = candidates[0].asJsonObject
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")[0].asJsonObject
                    .get("text").asString

                Log.d(TAG, "✓ LLM response")
                return@withContext Result.success(text)
            } catch (e: Exception) {
                Log.e(TAG, "LLM error: ${e.message}")
                if (retryCount < maxRetries - 1) {
                    retryCount++
                    delay(1000L)
                    continue
                }
                return@withContext Result.failure(e)
            }
        }
        
        return@withContext Result.failure(Exception("Max retries exceeded"))
    }

    suspend fun queryDMVHandbook(userQuery: String, handbookContext: String): Result<String> {
        return chatCompletion(listOf(
            ChatMessage("system", "Answer from handbook."),
            ChatMessage("user", "Q: $userQuery\nContext: ${handbookContext.take(1000)}")
        ), maxTokens = 512)
    }

    suspend fun queryFoodSecurity(userQuery: String, documentContext: String): Result<String> {
        return chatCompletion(listOf(
            ChatMessage("system", "Answer from context."),
            ChatMessage("user", "Q: $userQuery\nContext: ${documentContext.take(1000)}")
        ), maxTokens = 512)
    }

    suspend fun queryStockAnalysis(prompt: String): Result<String> {
        return chatCompletion(listOf(
            ChatMessage("system", "Analyst"),
            ChatMessage("user", prompt.take(1000))
        ), maxTokens = 512)
    }

    suspend fun queryMarketResearch(prompt: String): Result<String> {
        return chatCompletion(listOf(
            ChatMessage("system", "Market analyst"),
            ChatMessage("user", prompt.take(1000))
        ), maxTokens = 512)
    }

    suspend fun describeIndicator(indicatorName: String, dataDescription: String): Result<String> {
        return chatCompletion(listOf(
            ChatMessage("system", "Analyst"),
            ChatMessage("user", "Describe $indicatorName: ${dataDescription.take(500)}")
        ), maxTokens = 512)
    }
}

