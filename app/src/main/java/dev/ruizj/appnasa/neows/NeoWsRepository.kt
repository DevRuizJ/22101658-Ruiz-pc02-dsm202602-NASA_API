package dev.ruizj.appnasa.neows

import dev.ruizj.appnasa.BuildConfig
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeParseException

class NeoWsRepository {
    fun fetch(startDate: LocalDate, endDate: LocalDate): NeoWsFetchResult {
        val requestUrl = buildString {
            append(BuildConfig.NEOWS_BASE_URL)
            append(BuildConfig.NEOWS_FEED_ENDPOINT)
            append("?start_date=")
            append(encode(startDate.toString()))
            append("&end_date=")
            append(encode(endDate.toString()))
            append("&api_key=")
            append(encode(BuildConfig.NASA_API_KEY))
        }
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("NASA NeoWs request failed with HTTP $responseCode.")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val result = parseFeed(response, startDate, endDate)
            return result.copy(items = result.items.sortedByRecentApproach())
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

data class NeoWsFetchResult(
    val items: List<NearEarthObject>,
    val incompleteRecords: Int,
)

internal fun parseFeed(
    response: String,
    startDate: LocalDate,
    endDate: LocalDate,
): NeoWsFetchResult {
    val parsedRoot = JsonParser.parseString(response)
    val root = parsedRoot.takeIf(JsonElement::isJsonObject)?.asJsonObject
        ?: throw IOException("NASA NeoWs response is not a JSON object.")
    val groupedObjects = root.objectOrNull("near_earth_objects")
        ?: throw IOException("NASA NeoWs response is missing 'near_earth_objects'.")

    var incompleteRecords = 0
    val items = buildList {
        for ((date, groupedObjectsForDate) in groupedObjects.entrySet()) {
            val parsedDate = parseDate(date)
            if (parsedDate == null) {
                incompleteRecords++
                continue
            }
            if (parsedDate < startDate || parsedDate > endDate) continue

            val asteroidsForDate = groupedObjectsForDate
                .takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
            if (asteroidsForDate == null) {
                incompleteRecords++
                continue
            }

            for (asteroidElement in asteroidsForDate) {
                val asteroid = asteroidElement
                    .takeIf(JsonElement::isJsonObject)
                    ?.asJsonObject
                if (asteroid == null) {
                    incompleteRecords++
                    continue
                }

                val id = asteroid.optionalString("id")
                if (id == null) {
                    incompleteRecords++
                    continue
                }

                val approaches = asteroid.arrayOrNull("close_approach_data")
                val approachObjects = approaches
                    ?.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
                val approach = approachObjects
                    ?.firstOrNull { it.optionalString("close_approach_date") == date }
                    ?: approachObjects?.firstOrNull()
                val diameter = asteroid.objectOrNull("estimated_diameter")
                    ?.objectOrNull("meters")
                val velocity = approach?.objectOrNull("relative_velocity")
                val distance = approach?.objectOrNull("miss_distance")
                val name = asteroid.optionalString("name")
                val diameterMin = diameter?.optionalDouble("estimated_diameter_min")
                val diameterMax = diameter?.optionalDouble("estimated_diameter_max")
                val velocityKph = velocity?.optionalDouble("kilometers_per_hour")
                val missDistanceKm = distance?.optionalDouble("kilometers")
                val isHazardous = asteroid.optionalBoolean(
                    "is_potentially_hazardous_asteroid",
                )
                if (
                    name == null || diameterMin == null || diameterMax == null ||
                    velocityKph == null || missDistanceKm == null || isHazardous == null
                ) {
                    incompleteRecords++
                }

                add(
                    NearEarthObject(
                        id = id,
                        name = name ?: "Asteroide sin nombre",
                        closeApproachDate = date,
                        estimatedDiameterMinMeters = diameterMin,
                        estimatedDiameterMaxMeters = diameterMax,
                        relativeVelocityKph = velocityKph,
                        missDistanceKm = missDistanceKm,
                        isPotentiallyHazardous = isHazardous,
                    ),
                )
            }
        }
    }
    return NeoWsFetchResult(items, incompleteRecords)
}

private fun JsonObject.optionalString(key: String): String? =
    get(key)
        ?.takeIf(JsonElement::isJsonPrimitive)
        ?.asJsonPrimitive
        ?.takeIf { it.isString || it.isNumber }
        ?.asString
        ?.takeUnless(String::isBlank)

private fun JsonObject.optionalDouble(key: String): Double? {
    val value = optionalString(key)?.toDoubleOrNull()
        ?.takeIf(Double::isFinite)
    return value
}

private fun JsonObject.optionalBoolean(key: String): Boolean? =
    get(key)
        ?.takeIf(JsonElement::isJsonPrimitive)
        ?.asJsonPrimitive
        ?.takeIf { it.isBoolean }
        ?.asBoolean

private fun JsonObject.objectOrNull(key: String): JsonObject? =
    get(key)?.takeIf(JsonElement::isJsonObject)?.asJsonObject

private fun JsonObject.arrayOrNull(key: String) =
    get(key)?.takeIf(JsonElement::isJsonArray)?.asJsonArray

private fun parseDate(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (_: DateTimeParseException) {
    null
}
