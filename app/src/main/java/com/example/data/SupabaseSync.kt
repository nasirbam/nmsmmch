package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object SupabaseSync {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private const val TABLE_ENDPOINT = "/rest/v1/nms_state"
    private const val STATE_ROW_ID = "main"

    enum class SyncStatus {
        IDLE,
        SYNCING,
        SYNCED,
        OFFLINE
    }

    sealed class FetchResult {
        data class Found(val dataJson: String) : FetchResult()
        data object Empty : FetchResult()
        data class Error(val message: String) : FetchResult()
    }

    suspend fun fetchState(): FetchResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext FetchResult.Error("Supabase configuration missing")
        }

        val request = Request.Builder()
            .url("$baseUrl$TABLE_ENDPOINT?id=eq.$STATE_ROW_ID&select=data")
            .get()
            .addHeader("apikey", apiKey)
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Error("HTTP ${response.code}: $bodyStr")
                }
                val arr = JSONArray(bodyStr)
                if (arr.length() == 0) {
                    return@withContext FetchResult.Empty
                }
                val row = arr.getJSONObject(0)
                val dataObj = row.optJSONObject("data")
                if (dataObj != null) {
                    FetchResult.Found(dataObj.toString())
                } else {
                    FetchResult.Empty
                }
            }
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun pushState(dataJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return@withContext try {
            val payload = JSONObject().apply {
                put("id", STATE_ROW_ID)
                put("data", JSONObject(dataJson))
                put("updated_at", isoNowUtc())
            }

            val request = Request.Builder()
                .url("$baseUrl$TABLE_ENDPOINT?on_conflict=id")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .addHeader("apikey", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errBody = response.body?.string().orEmpty()
                    Result.failure(RuntimeException("HTTP ${response.code}: $errBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isoNowUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
