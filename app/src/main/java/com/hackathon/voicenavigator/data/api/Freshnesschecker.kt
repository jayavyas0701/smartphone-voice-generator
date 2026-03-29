package com.hackathon.voicenavigator.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight freshness checker for source documents.
 *
 * Strategy: HTTP HEAD request (downloads 0 bytes) → read Last-Modified header
 * → compare against stored date → show update banner if newer.
 *
 * Fails silently on any error — never crashes, never blocks the demo.
 */
object FreshnessChecker {

    private const val TAG = "FreshnessChecker"
    private const val PREFS_NAME = "freshness_prefs"
    private const val TIMEOUT_MS = 5_000

    // Source document URLs
    const val DMV_PDF_URL = "https://www.dmv.ca.gov/portal/file/california-driver-handbook-pdf/"
    const val SOFI_2024_URL = "https://openknowledge.fao.org/handle/20.500.14283/cd1254en"
    const val SOFI_2023_URL = "https://openknowledge.fao.org/server/api/core/bitstreams/cf229117-e097-40d1-9e2c-cb2e85e7e8d7/content"

    // Human-readable source info
    const val DMV_SOURCE_LABEL = "Source: CA DMV Handbook, Rev. 6/2025 (DL-600)"
    const val ESG_SOURCE_LABEL = "Source: FAO SOFI Reports 2023 & 2024"

    data class FreshnessResult(
        val sourceKey: String,
        val isUpdated: Boolean,
        val lastModified: String?,
        val previousModified: String?
    )

    suspend fun checkFreshness(
        context: Context,
        sourceKey: String,
        documentUrl: String
    ): FreshnessResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(documentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = true

            val statusCode = conn.responseCode
            conn.disconnect()

            if (statusCode !in 200..399) {
                Log.w(TAG, "HEAD $documentUrl → HTTP $statusCode")
                return@withContext FreshnessResult(sourceKey, false, null, null)
            }

            val lastModified = conn.getHeaderField("Last-Modified")
            val contentLength = conn.getHeaderField("Content-Length")
            val fingerprint = lastModified ?: contentLength ?: ""

            if (fingerprint.isBlank()) {
                Log.d(TAG, "No Last-Modified or Content-Length header for $sourceKey")
                return@withContext FreshnessResult(sourceKey, false, null, null)
            }

            val prefs = getPrefs(context)
            val storedFingerprint = prefs.getString("${sourceKey}_fingerprint", null)

            return@withContext if (storedFingerprint == null) {
                prefs.edit().putString("${sourceKey}_fingerprint", fingerprint).apply()
                Log.d(TAG, "✓ First freshness check for $sourceKey: $fingerprint")
                FreshnessResult(sourceKey, false, fingerprint, null)
            } else if (storedFingerprint != fingerprint) {
                Log.d(TAG, "⚠ $sourceKey updated: was '$storedFingerprint', now '$fingerprint'")
                FreshnessResult(sourceKey, true, fingerprint, storedFingerprint)
            } else {
                Log.d(TAG, "✓ $sourceKey unchanged: $fingerprint")
                FreshnessResult(sourceKey, false, fingerprint, storedFingerprint)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Freshness check failed for $sourceKey: ${e.message}")
            FreshnessResult(sourceKey, false, null, null)
        }
    }

    fun acknowledgeUpdate(context: Context, sourceKey: String, newFingerprint: String) {
        getPrefs(context).edit().putString("${sourceKey}_fingerprint", newFingerprint).apply()
        Log.d(TAG, "User acknowledged update for $sourceKey")
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}