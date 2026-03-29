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
 * Bureau of Labor Statistics (BLS) API Service
 * Fetches price data for gasoline, milk, and other consumer products
 *
 * API: https://api.bls.gov/publicAPI/v2/timeseries/data/
 *
 * Key Series IDs:
 * - APU000074714: US Regular All Formulations Gas Price
 * - APU0000709112: Average Price: Milk, Fresh, Whole, Fortified (per gallon)
 */
object BLSApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Optional: Set BLS API registration key for higher rate limits
    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    /**
     * Fetch time series data from BLS
     * @param seriesId The BLS series ID
     * @param startYear Start year for data range
     * @param endYear End year for data range
     */
    suspend fun fetchTimeSeries(
        seriesId: String,
        startYear: Int = 1992,
        endYear: Int = 2026
    ): Result<ChartData> = withContext(Dispatchers.IO) {
        try {
            // BLS API limits to 20-year ranges per request
            val allDataPoints = mutableListOf<ChartDataPoint>()

            // Fetch in 20-year chunks
            var currentStart = startYear
            while (currentStart <= endYear) {
                val currentEnd = minOf(currentStart + 19, endYear)
                val requestBody = buildString {
                    append("{\"seriesid\":[\"$seriesId\"],")
                    append("\"startyear\":\"$currentStart\",")
                    append("\"endyear\":\"$currentEnd\"")
                    apiKey?.let { append(",\"registrationkey\":\"$it\"") }
                    append("}")
                }

                val request = Request.Builder()
                    .url("https://api.bls.gov/publicAPI/v2/timeseries/data/")
                    .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val blsResponse = gson.fromJson(body, BLSResponse::class.java)
                    blsResponse.Results?.series?.firstOrNull()?.data?.forEach { dataPoint ->
                        val year = dataPoint.year ?: return@forEach
                        val period = dataPoint.period ?: return@forEach
                        val value = dataPoint.value?.toDoubleOrNull() ?: return@forEach
                        val monthName = dataPoint.periodName ?: ""

                        // Convert period (M01, M02, etc.) to month number
                        if (period.startsWith("M") && period != "M13") {
                            val monthNum = period.substring(1).toIntOrNull() ?: return@forEach
                            val dateLabel = "$year-${monthNum.toString().padStart(2, '0')}"
                            allDataPoints.add(ChartDataPoint(year = dateLabel, value = value))
                        }
                    }
                }

                currentStart = currentEnd + 1
            }

            // Sort chronologically
            allDataPoints.sortBy { it.year }

            val title = when (seriesId) {
                "APU000074714" -> "US Regular Gas Price"
                "APU0000709112" -> "Milk Price (per gallon)"
                else -> "Price Data"
            }

            val unit = "U.S. Dollars"

            Result.success(
                ChartData(
                    title = title,
                    unit = unit,
                    dataPoints = allDataPoints
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch US Regular Gas Price
     */
    suspend fun fetchGasPrice(startYear: Int = 1992): Result<ChartData> =
        fetchTimeSeries("APU000074714", startYear)

    /**
     * Fetch Milk Price
     */
    suspend fun fetchMilkPrice(startYear: Int = 1992): Result<ChartData> =
        fetchTimeSeries("APU0000709112", startYear)
}
