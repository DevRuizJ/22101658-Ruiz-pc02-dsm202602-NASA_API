package dev.ruizj.appnasa.apod

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import dev.ruizj.appnasa.BuildConfig
import dev.ruizj.appnasa.data.ApodApi
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ApodFetchResult(
    val items: List<ApodItem>,
    val incompleteRecords: Int,
)

class ApodRepository(
    private val api: ApodApi,
    private val dao: ApodDao,
    private val gson: Gson = Gson(),
) {
    fun observeItems(): Flow<List<ApodItem>> = dao.observeAll().map { rows ->
        rows.map(ApodEntity::toDomain)
    }

    fun observeFavorites(): Flow<List<ApodItem>> = dao.observeFavorites().map { rows ->
        rows.map(ApodEntity::toDomain)
    }

    suspend fun refresh(startDate: LocalDate, endDate: LocalDate): Int {
        val response = api.getApod(
            apiKey = BuildConfig.NASA_API_KEY,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
        )
        if (!response.isSuccessful) {
            throw IOException("NASA APOD request failed with HTTP ${response.code()}.")
        }
        val body = response.body()
            ?: throw IOException("NASA APOD response body is empty.")
        val result = mapApodResponse(body, startDate, endDate, gson)
        dao.upsertFromApi(result.items)
        return result.incompleteRecords
    }

    suspend fun toggleFavorite(date: String) {
        if (dao.toggleFavorite(date) == 0) {
            throw IOException("APOD item '$date' is no longer available.")
        }
    }

}

internal fun mapApodResponse(
    body: JsonElement,
    startDate: LocalDate,
    endDate: LocalDate,
    gson: Gson = Gson(),
): ApodFetchResult {
    val jsonItems = body.takeIf(JsonElement::isJsonArray)?.asJsonArray
        ?: throw IOException("NASA APOD response is not a JSON array.")
    var incompleteRecords = 0
    val items = buildList {
        for (element in jsonItems) {
            if (!element.isJsonObject) {
                incompleteRecords++
                continue
            }
            val dto = gson.fromJson(element, ApodDto::class.java)
            val item = dto.toDomainOrNull()
            if (item == null) {
                incompleteRecords++
                continue
            }
            val date = try {
                LocalDate.parse(item.date)
            } catch (_: DateTimeParseException) {
                incompleteRecords++
                continue
            }
            if (date < startDate || date > endDate) continue
            if (
                dto.title.isNullOrBlank() ||
                dto.explanation.isNullOrBlank() ||
                dto.mediaType !in setOf("image", "video") ||
                (dto.mediaType == "image" && dto.hdurl.isNullOrBlank())
            ) {
                incompleteRecords++
            }
            add(item)
        }
    }.sortedByDescending(ApodItem::date)
    return ApodFetchResult(items, incompleteRecords)
}

internal fun parseApodResponse(
    response: String,
    startDate: LocalDate,
    endDate: LocalDate,
): ApodFetchResult = mapApodResponse(
    JsonParser.parseString(response),
    startDate,
    endDate,
)
