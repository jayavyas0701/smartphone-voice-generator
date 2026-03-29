package com.hackathon.voicenavigator.data.api

import com.google.gson.Gson
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI API Service for RAG (Retrieval Augmented Generation)
 * Used for both Food Security analysis and DMV Knowledge Test
 */
object OpenAIApiService {

    // ⚠️ IMPORTANT: Replace with your actual OpenAI API key
    private var apiKey: String = "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"

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
     * Send a chat completion request to OpenAI
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String = "gpt-3.5-turbo",
        temperature: Double = 0.7,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = gson.toJson(
                ChatRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    max_tokens = maxTokens
                )
            )

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("OpenAI API error ${response.code}: $body")
                )
            }

            val chatResponse = gson.fromJson(body, ChatResponse::class.java)
            val content = chatResponse.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty response from OpenAI"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * RAG-based query for Food Security reports
     * Combines context from PDF with LLM for grounded answers
     */
    suspend fun queryFoodSecurity(
        userQuery: String,
        documentContext: String
    ): Result<String> {
        val systemPrompt = """You are an expert ESG analyst specializing in food security and nutrition. 
You analyze data from the FAO's "The State of Food Security and Nutrition in the World" reports (2023, 2024, 2025).

IMPORTANT: Answer ONLY based on the provided document context. Do not use external knowledge.
If the answer is not in the context, say "This information is not available in the provided reports."

Provide specific statistics, numbers, and data points when available.
Format your response clearly with key findings highlighted."""

        val messages = listOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", """
Based on the following document context:

---
$documentContext
---

User Question: $userQuery

Please provide a detailed, data-driven answer based solely on the document context above.
            """.trimIndent())
        )

        return chatCompletion(messages, maxTokens = 2048)
    }

    /**
     * RAG-based query for California DMV Knowledge Test
     * Uses California Driver's Handbook as source
     */
    suspend fun queryDMVHandbook(
        userQuery: String,
        handbookContext: String
    ): Result<String> {
        val systemPrompt = """You are a California DMV Knowledge Test preparation assistant.
You help users prepare for the California Driver's License knowledge test using ONLY information 
from the official California Driver's Handbook.

IMPORTANT: Answer ONLY based on the provided handbook context. Do not use external knowledge.
Include relevant rules, regulations, and safety information.
If asked about specific topics (signaling, BAC limits, speed limits, etc.), provide complete details."""

        val messages = listOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", """
Based on the California Driver's Handbook context:

---
$handbookContext
---

User Question: $userQuery

Provide an accurate, detailed answer for DMV test preparation.
            """.trimIndent())
        )

        return chatCompletion(messages, maxTokens = 2048)
    }

    /**
     * Generate stock/DOW analysis via ChatGPT
     */
    suspend fun queryStockAnalysis(prompt: String): Result<String> {
        val messages = listOf(
            ChatMessage("system", """You are a financial market analyst. Provide accurate market data.
When asked about Top 10 DOW stocks, respond in JSON format like:
{"stocks": [{"name": "Apple", "ticker": "AAPL", "percentage": 9.2}, ...]}
Include "The Rest" category for remaining stocks."""),
            ChatMessage("user", prompt)
        )

        return chatCompletion(messages, maxTokens = 1024)
    }

    /**
     * General market research query via ChatGPT
     */
    suspend fun queryMarketResearch(prompt: String): Result<String> {
        val messages = listOf(
            ChatMessage("system", "You are a market research analyst. Provide data-driven insights with specific numbers and trends."),
            ChatMessage("user", prompt)
        )

        return chatCompletion(messages, maxTokens = 2048)
    }

    /**
     * Describe an ESG indicator using ChatGPT
     */
    suspend fun describeIndicator(
        indicatorName: String,
        dataDescription: String
    ): Result<String> {
        val messages = listOf(
            ChatMessage("system", "You are an ESG data analyst. Explain economic and environmental indicators clearly with context about global trends."),
            ChatMessage("user", "Describe $indicatorName. Here is the data context: $dataDescription")
        )

        return chatCompletion(messages, maxTokens = 1024)
    }
}
