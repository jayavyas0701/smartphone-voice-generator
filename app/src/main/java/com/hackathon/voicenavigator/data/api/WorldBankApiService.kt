package com.hackathon.voicenavigator.data.api

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.hackathon.voicenavigator.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * World Bank API Service
 * Fetches ESG indicators: GDP, CO2, Agricultural Land
 * Base URL: https://api.worldbank.org/v2/
 */
object WorldBankApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetch indicator data from World Bank API
     * @param indicator The ESG indicator to fetch
     * @param country Country code (default: WLD for World)
     * @param perPage Number of records per page
     */
    suspend fun fetchIndicatorData(
        indicator: ESGIndicator,
        country: String = "WLD",
        perPage: Int = 100
    ): Result<ChartData> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.worldbank.org/v2/country/$country/indicator/${indicator.apiCode}?format=json&per_page=$perPage"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API call failed: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val jsonArray = JsonParser.parseString(body).asJsonArray

            if (jsonArray.size() < 2) {
                return@withContext Result.failure(Exception("No data available"))
            }

            val dataArray = jsonArray[1].asJsonArray
            val dataPoints = mutableListOf<ChartDataPoint>()

            for (element in dataArray) {
                val obj = element.asJsonObject
                val date = obj.get("date")?.asString ?: continue
                val value = if (obj.get("value")?.isJsonNull == false) {
                    obj.get("value")?.asDouble
                } else null

                if (value != null) {
                    dataPoints.add(ChartDataPoint(year = date, value = value))
                }
            }

            // Sort by year ascending
            dataPoints.sortBy { it.year }

            Result.success(
                ChartData(
                    title = indicator.displayName,
                    unit = indicator.unit,
                    dataPoints = dataPoints,
                    indicatorDescription = indicator.description
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch GDP data specifically
     */
    suspend fun fetchGDPData(): Result<ChartData> = fetchIndicatorData(ESGIndicator.GDP)

    /**
     * Fetch CO2 emissions data
     */
    suspend fun fetchCO2Data(): Result<ChartData> = fetchIndicatorData(ESGIndicator.CO2)

    /**
     * Fetch Agricultural Land data
     */
    suspend fun fetchAgriLandData(): Result<ChartData> = fetchIndicatorData(ESGIndicator.AGRI_LAND)

    /**
     * Fetch CO2 Per Capita data
     */
    suspend fun fetchCO2PerCapitaData(): Result<ChartData> = fetchIndicatorData(ESGIndicator.CO2_PER_CAPITA)
}
