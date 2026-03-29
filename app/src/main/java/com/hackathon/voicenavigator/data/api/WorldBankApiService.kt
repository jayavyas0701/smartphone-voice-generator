package com.hackathon.voicenavigator.data.api

import android.util.Log
import com.hackathon.voicenavigator.data.model.ChartData
import com.hackathon.voicenavigator.data.model.ChartDataPoint
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object WorldBankApiService {

    private const val TAG = "WorldBankApiService"
    private const val BASE = "https://api.worldbank.org/v2/country/WLD/indicator"
    private const val PARAMS = "?format=json&per_page=20&mrv=20"

    suspend fun fetchGDPData(): Result<ChartData> =
        fetch("NY.GDP.MKTP.KD.ZG", "Global GDP Growth Rate (%)", "% Annual Growth")

    suspend fun fetchCO2Data(): Result<ChartData> =
        fetch("EN.ATM.CO2E.KT", "Global CO₂ Emissions (kt)", "kt CO₂")

    suspend fun fetchAgriLandData(): Result<ChartData> =
        fetch("AG.LND.AGRI.ZS", "Agricultural Land (% of land area)", "% of land")

    suspend fun fetchCO2PerCapitaData(): Result<ChartData> =
        fetch("EN.ATM.CO2E.PC", "CO₂ Emissions per Capita", "t per person")

    private fun fetch(indicator: String, title: String, unit: String): Result<ChartData> {
        return try {
            val url = URL("$BASE/$indicator$PARAMS")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout    = 20_000
            conn.requestMethod  = "GET"

            val statusCode = conn.responseCode
            if (statusCode !in 200..299) {
                return Result.failure(RuntimeException("World Bank API HTTP $statusCode"))
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()

            val jsonArray = JSONArray(body)
            if (jsonArray.length() < 2) {
                return Result.failure(RuntimeException("Unexpected World Bank response format"))
            }

            val dataArray = jsonArray.getJSONArray(1)
            val points = mutableListOf<ChartDataPoint>()

            for (i in 0 until dataArray.length()) {
                val item  = dataArray.getJSONObject(i)
                val year  = item.optString("date", "")
                val value = item.optString("value", "")
                if (year.isNotBlank() && value.isNotBlank() && value != "null") {
                    runCatching { points.add(ChartDataPoint(year, value.toDouble())) }
                }
            }

            if (points.isEmpty()) {
                return Result.failure(RuntimeException("No data returned for $indicator — World Bank may have changed this series"))
            }

            val sorted = points.sortedBy { it.year }
            Log.d(TAG, "Fetched $indicator: ${sorted.size} data points")
            Result.success(ChartData(title, unit, sorted))

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $indicator: ${e.message}")
            Result.failure(e)
        }
    }
}