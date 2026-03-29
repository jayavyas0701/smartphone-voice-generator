package com.hackathon.voicenavigator.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
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

    /**
     * Generate embedding vector for text using Google's Gemini embedding API.
     * Model: embedding-001 (768 dimensions, free tier available)
     * 
     * API Reference: https://ai.google.dev/api/rest/v1beta/models/embedContent
     */
    suspend fun generateEmbedding(text: String): List<Double>? = withContext(Dispatchers.IO) {
        try {
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
            android.util.Log.d("GeminiApiService", "Embedding text ${index + 1}/${texts.size}")
            generateEmbedding(text)
        }
    }
}

