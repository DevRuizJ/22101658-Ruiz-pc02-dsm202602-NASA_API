package dev.ruizj.appnasa.apod

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

class ApodRepository {
    fun fetch(startDate: LocalDate, endDate: LocalDate): ApodFetchResult {
        val requestUrl = buildString {
            append(BuildConfig.APOD_BASE_URL)
            append(BuildConfig.APOD_ENDPOINT)
            append("?api_key=")
            append(encode(BuildConfig.NASA_API_KEY))
            append("&start_date=")
            append(encode(startDate.toString()))
            append("&end_date=")
            append(encode(endDate.toString()))
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
                throw IOException("NASA APOD request failed with HTTP $responseCode.")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseApodResponse(response, startDate, endDate)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}

data class ApodFetchResult(
    val items: List<ApodItem>,
    val incompleteRecords: Int,
)

internal fun parseApodResponse(
    response: String,
    startDate: LocalDate,
    endDate: LocalDate,
): ApodFetchResult {
    val parsedResponse = JsonParser.parseString(response)
    val jsonItems = parsedResponse.takeIf(JsonElement::isJsonArray)?.asJsonArray
        ?: throw IOException("NASA APOD response is not a JSON array.")
    var incompleteRecords = 0
    val items = buildList {
        for (itemElement in jsonItems) {
            val item = itemElement.takeIf(JsonElement::isJsonObject)?.asJsonObject
            if (item == null) {
                incompleteRecords++
                continue
            }
            val date = item.optionalString("date")
            val parsedDate = date?.let {
                try {
                    LocalDate.parse(it)
                } catch (_: DateTimeParseException) {
                    null
                }
            }
            if (parsedDate == null) {
                incompleteRecords++
                continue
            }
            if (parsedDate < startDate || parsedDate > endDate) continue

            val title = item.optionalString("title")
            val explanation = item.optionalString("explanation")
            val mediaType = item.optionalString("media_type")
                ?.takeIf { it == "image" || it == "video" }
            val hdUrl = item.optionalString("hdurl")
            if (title == null || explanation == null || mediaType == null ||
                (mediaType == "image" && hdUrl == null)
            ) {
                incompleteRecords++
            }
            add(
                ApodItem(
                    date = parsedDate.toString(),
                    title = title ?: "Sin título",
                    explanation = explanation ?: "Descripción no disponible.",
                    mediaType = mediaType ?: "unknown",
                    imageUrl = hdUrl.takeIf { mediaType == "image" },
                    credit = item.optionalString("credit")
                        ?: item.optionalString("copyright"),
                ),
            )
        }
    }.sortedByDescending(ApodItem::date)
    return ApodFetchResult(items, incompleteRecords)
}

private fun JsonObject.optionalString(key: String): String? =
    get(key)
        ?.takeIf(JsonElement::isJsonPrimitive)
        ?.asJsonPrimitive
        ?.takeIf { it.isString || it.isNumber }
        ?.asString
        ?.takeUnless(String::isBlank)

fun List<ApodItem>.filterApodRange(
    startDate: LocalDate,
    endDate: LocalDate,
): List<ApodItem> = filter { item ->
    val date = LocalDate.parse(item.date)
    date >= startDate && date <= endDate
}.sortedByDescending { it.date }
