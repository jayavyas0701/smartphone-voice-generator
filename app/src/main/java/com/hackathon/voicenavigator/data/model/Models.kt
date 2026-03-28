package com.hackathon.voicenavigator.data.model

import com.google.gson.annotations.SerializedName

// ── World Bank API Models ──────────────────────────────────────────
data class WorldBankResponse(
    val page: Int?,
    val pages: Int?,
    val total: Int?
)

data class WorldBankDataPoint(
    val indicator: WorldBankIndicator?,
    val country: WorldBankCountry?,
    val countryiso3code: String?,
    val date: String?,
    val value: Double?,
    val unit: String?,
    val obs_status: String?,
    val decimal: Int?
)

data class WorldBankIndicator(
    val id: String?,
    val value: String?
)

data class WorldBankCountry(
    val id: String?,
    val value: String?
)

// ── Chart Data Models ──────────────────────────────────────────────
data class ChartDataPoint(
    val year: String,
    val value: Double
)

data class ChartData(
    val title: String,
    val unit: String,
    val dataPoints: List<ChartDataPoint>,
    val indicatorDescription: String = ""
)

// ── OpenAI / ChatGPT Models ───────────────────────────────────────
data class ChatMessage(
    val role: String,   // "user", "assistant", "system"
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024
)

data class ChatResponse(
    val id: String?,
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    val index: Int?,
    val message: ChatMessage?,
    val finish_reason: String?
)

// ── Stock/DOW Data Models ─────────────────────────────────────────
data class StockData(
    val name: String,
    val ticker: String,
    val percentage: Double
)

// ── DMV Models ────────────────────────────────────────────────────
data class DMVQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val category: String
)

data class DMVQuizResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val score: Double
)

// ── BLS (Bureau of Labor Statistics) API Models ───────────────────
data class BLSResponse(
    val status: String?,
    val Results: BLSResults?
)

data class BLSResults(
    val series: List<BLSSeries>?
)

data class BLSSeries(
    val seriesID: String?,
    val data: List<BLSDataPoint>?
)

data class BLSDataPoint(
    val year: String?,
    val period: String?,
    val periodName: String?,
    val value: String?,
    val footnotes: List<Any>?
)

// ── Food Security RAG Models ──────────────────────────────────────
data class RAGResponse(
    val answer: String,
    val sources: List<String> = emptyList(),
    val confidence: Double = 0.0
)

// ── ESG Indicator Types ───────────────────────────────────────────
enum class ESGIndicator(
    val apiCode: String,
    val displayName: String,
    val unit: String,
    val description: String
) {
    GDP(
        "NY.GDP.MKTP.KD.ZG",
        "GDP Growth Rate",
        "%",
        "Annual percentage growth rate of GDP at market prices based on constant local currency"
    ),
    CO2(
        "EN.GHG.CO2.AG.MT.CE.AR5",
        "CO2 Emissions (Agriculture)",
        "Thousand Mt",
        "Carbon dioxide emissions from agriculture, measured in thousand metric tons"
    ),
    AGRI_LAND(
        "AG.LND.AGRI.ZS",
        "Agricultural Land",
        "% of land area",
        "Agricultural land refers to the share of land area that is arable, under permanent crops, or permanent pastures"
    ),
    CO2_PER_CAPITA(
        "EN.ATM.CO2E.PC",
        "CO2 Emissions Per Capita",
        "Metric tons",
        "Carbon dioxide emissions per capita from burning of fossil fuels and cement manufacture"
    )
}
