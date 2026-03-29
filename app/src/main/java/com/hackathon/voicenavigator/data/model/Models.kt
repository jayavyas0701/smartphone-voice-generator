package com.hackathon.voicenavigator.data.model

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChartDataPoint(
    val year: String, // Changed to String to support monthly labels from BLS (e.g., "2023-01")
    val value: Double
)

data class ChartData(
    val title: String,
    val unit: String,
    val dataPoints: List<ChartDataPoint>
)

enum class ESGIndicator(val displayName: String) {
    GDP("GDP Growth"),
    CO2("CO₂ Emissions"),
    AGRI_LAND("Agri Land"),
    CO2_PER_CAPITA("CO₂/Capita")
}

data class StockData(
    val name: String,
    val ticker: String,
    val percentage: Double
)

data class DMVQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val category: String
)

/**
 * BLS API Response Models
 */
data class BLSResponse(
    val status: String? = null,
    val responseTime: Int? = null,
    val message: List<String>? = null,
    val Results: BLSResults? = null
)

data class BLSResults(
    val series: List<BLSSeries>? = null
)

data class BLSSeries(
    val seriesID: String? = null,
    val data: List<BLSDataPoint>? = null
)

data class BLSDataPoint(
    val year: String? = null,
    val period: String? = null,
    val periodName: String? = null,
    val value: String? = null,
    val footnotes: List<BLSFootnote>? = null
)

data class BLSFootnote(
    val code: String? = null,
    val text: String? = null
)
