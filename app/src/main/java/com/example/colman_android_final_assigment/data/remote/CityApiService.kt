package com.example.colman_android_final_assigment.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Singleton service that fetches Israeli city names from the government open-data API.
 * Results are cached in-memory so each city ID is looked up at most once per app session.
 *
 * API: https://data.gov.il/api/action/datastore_search
 * Dataset resource_id: b7cf8f14-64a2-4b33-8d4b-edb286fdbd37
 */
object CityApiService {

    private const val BASE_URL = "https://data.gov.il/api/action/datastore_search"
    private const val RESOURCE_ID = "b7cf8f14-64a2-4b33-8d4b-edb286fdbd37"

    /** In-memory cache: cityId -> English city name */
    private val cache = mutableMapOf<Int, String>()

    /**
     * Returns the English city name for the given [cityId],
     * or "City not found" if the API has no matching record.
     */
    suspend fun getCityNameById(cityId: Int): String {
        // Return cached value if available
        cache[cityId]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val filters = URLEncoder.encode(
                    """{"סמל_ישוב":$cityId}""",
                    "UTF-8"
                )
                val urlString = "$BASE_URL?resource_id=$RESOURCE_ID&filters=$filters"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext "City not found"
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(responseBody)
                val success = json.optBoolean("success", false)
                if (!success) {
                    return@withContext "City not found"
                }

                val records = json.getJSONObject("result").getJSONArray("records")
                if (records.length() == 0) {
                    return@withContext "City not found"
                }

                val record = records.getJSONObject(0)
                val englishName = record.optString("שם_ישוב_לועזי", "").trim()

                val cityName = englishName.ifEmpty { "City not found" }
                // Cache the result
                cache[cityId] = cityName
                cityName
            } catch (e: Exception) {
                "City not found"
            }
        }
    }

    /** Pre-fetch multiple city IDs at once (useful for lists). */
    suspend fun prefetchCities(cityIds: List<Int>) {
        val uncached = cityIds.distinct().filter { it !in cache }
        // Fetch each uncached city (the API only supports single-record filters)
        uncached.forEach { getCityNameById(it) }
    }

    /** Clear the in-memory cache (e.g. on logout). */
    fun clearCache() {
        cache.clear()
    }
}

