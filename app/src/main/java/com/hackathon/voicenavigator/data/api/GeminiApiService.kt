package com.hackathon.voicenavigator.data.api

import android.util.Log
import com.hackathon.voicenavigator.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiApiService {

    private const val TAG = "GeminiApiService"

    // gemini-2.0-flash works with v1beta — confirmed from your earlier builds
    private const val MODEL = "gemini-2.5-flash-lite"
    private const val EMBED_MODEL = "text-embedding-004"

    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    private const val EMBED_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$EMBED_MODEL:embedContent"

    private const val MAX_RETRIES = 2
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000

    private var apiKey: String = ""

    private var quotaExhausted = false
    private var quotaResetTimeMs = 0L

    fun setApiKey(key: String) {
        apiKey = key.trim()
        quotaExhausted = false
        quotaResetTimeMs = 0L
        Log.d(TAG, "API key set (length=${apiKey.length})")
    }

    private fun isQuotaBlocked(): Boolean {
        if (!quotaExhausted) return false
        if (System.currentTimeMillis() > quotaResetTimeMs) {
            quotaExhausted = false
            return false
        }
        return true
    }

    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        maxTokens: Int = 1024
    ): Result<String> {
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("Gemini API key not set."))
        }

        if (isQuotaBlocked()) {
            val waitSec = ((quotaResetTimeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
            return Result.failure(QuotaExceededException(
                "API quota exceeded. Please wait ~${waitSec}s or use a new API key."
            ))
        }

        val systemText = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val turns = messages.filter { it.role != "system" }

        val body = JSONObject().apply {
            if (systemText.isNotBlank()) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemText) })
                    })
                })
            }
            put("contents", JSONArray().apply {
                turns.forEach { msg ->
                    val geminiRole = if (msg.role == "assistant") "model" else "user"
                    put(JSONObject().apply {
                        put("role", geminiRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.content) })
                        })
                    })
                }
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", maxTokens)
                put("temperature", 0.3)
            })
        }

        return callWithRetry(body)
    }

    suspend fun generateEmbedding(text: String): List<Double>? {
        if (apiKey.isEmpty()) return null
        if (isQuotaBlocked()) return null

        val body = JSONObject().apply {
            put("model", "models/$EMBED_MODEL")
            put("content", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            })
        }
        return try {
            val responseText = withContext(Dispatchers.IO) { postRawRequest(EMBED_URL, body) }
            val root = JSONObject(responseText)
            val values = root.getJSONObject("embedding").getJSONArray("values")
            (0 until values.length()).map { values.getDouble(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Embedding error: ${e.message}")
            null
        }
    }

    suspend fun queryMarketResearch(prompt: String): Result<String> =
        chatCompletion(listOf(ChatMessage("user", prompt)), maxTokens = 512)

    suspend fun describeIndicator(indicatorName: String, description: String = ""): Result<String> {
        val prompt = if (description.isNotBlank())
            "Describe the ESG indicator '$indicatorName': $description. Give a concise 3-4 sentence analysis."
        else
            "Describe the ESG indicator '$indicatorName'. Give a concise 3-4 sentence analysis."
        return chatCompletion(listOf(ChatMessage("user", prompt)), maxTokens = 512)
    }

    suspend fun queryStockAnalysis(prompt: String): Result<String> =
        chatCompletion(listOf(ChatMessage("user", prompt)), maxTokens = 512)

    private suspend fun callWithRetry(body: JSONObject): Result<String> {
        var lastError: Exception = RuntimeException("Unknown error")
        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                val backoffMs = 3000L * attempt
                Log.w(TAG, "Retry $attempt after ${backoffMs}ms")
                delay(backoffMs)
            }
            try {
                val raw = withContext(Dispatchers.IO) { postRawRequest(BASE_URL, body) }
                return Result.success(parseResponse(raw))
            } catch (e: GeminiApiException) {
                Log.e(TAG, "Gemini API error ${e.statusCode}: ${e.message}")
                if (e.statusCode == 429 || (e.message?.contains("quota", ignoreCase = true) == true)) {
                    markQuotaExhausted(e.message)
                    return Result.failure(QuotaExceededException(
                        "API quota exceeded. Please wait or use a new Gemini API key."
                    ))
                }
                return Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempt failed: ${e.javaClass.simpleName}: ${e.message}")
                lastError = e
            }
        }
        return Result.failure(RuntimeException("Max retries exceeded: ${lastError.message}", lastError))
    }

    private fun markQuotaExhausted(errorMsg: String?) {
        quotaExhausted = true
        val retrySeconds = try {
            val match = Regex("(\\d+\\.?\\d*)s").find(errorMsg ?: "")
            match?.groupValues?.get(1)?.toDouble()?.toLong() ?: 60
        } catch (e: Exception) { 60L }
        quotaResetTimeMs = System.currentTimeMillis() + (retrySeconds * 1000)
        Log.w(TAG, "Quota exhausted — blocking API calls for ${retrySeconds}s")
    }

    private fun postRawRequest(baseUrl: String, body: JSONObject): String {
        val url = URL("$baseUrl?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(body.toString())
                it.flush()
            }

            val statusCode = conn.responseCode
            return if (statusCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                    .use { it.readText() }
            } else {
                val errBody = try {
                    conn.errorStream?.let {
                        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
                    } ?: "no error body"
                } catch (e: Exception) { "could not read error: ${e.message}" }
                Log.e(TAG, "HTTP $statusCode: $errBody")
                throw GeminiApiException(statusCode, parseApiError(errBody, statusCode))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseResponse(json: String): String {
        return try {
            val root = JSONObject(json)
            if (root.has("error")) {
                val err = root.getJSONObject("error")
                throw GeminiApiException(err.optInt("code", -1), err.optString("message", "Unknown Gemini error"))
            }
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: GeminiApiException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}\nRaw: $json")
            throw RuntimeException("Failed to parse Gemini response: ${e.message}")
        }
    }

    private fun parseApiError(errorBody: String, statusCode: Int): String {
        return try {
            JSONObject(errorBody).getJSONObject("error").optString("message", "HTTP $statusCode")
        } catch (e: Exception) {
            "HTTP $statusCode: $errorBody"
        }
    }

    class GeminiApiException(val statusCode: Int, message: String) : Exception(message)
    class QuotaExceededException(message: String) : Exception(message)
}