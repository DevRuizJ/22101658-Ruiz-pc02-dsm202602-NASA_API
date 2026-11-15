package dev.ruizj.appnasa.neows

import com.google.gson.Gson
import dev.ruizj.appnasa.BuildConfig
import dev.ruizj.appnasa.data.NeoWsApi
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class NeoWsFetchResult(
    val items: List<NearEarthObject>,
    val incompleteRecords: Int,
)

class NeoWsRepository(
    private val api: NeoWsApi,
    private val dao: NeoWsDao,
) {
    fun observeItems(): Flow<List<NearEarthObject>> = dao.observeAll().map { rows ->
        rows.map(NeoWsEntity::toDomain)
    }

    fun observeFavorites(): Flow<List<NearEarthObject>> = dao.observeFavorites().map { rows ->
        rows.map(NeoWsEntity::toDomain)
    }

    suspend fun refresh(startDate: LocalDate, endDate: LocalDate): Int {
        val response = api.getFeed(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            apiKey = BuildConfig.NASA_API_KEY,
        )
        if (!response.isSuccessful) {
            throw IOException("NASA NeoWs request failed with HTTP ${response.code()}.")
        }
        val feed = response.body()
            ?: throw IOException("NASA NeoWs response body is empty.")
        val result = mapFeed(feed, startDate, endDate)
        dao.upsertFromApi(result.items)
        return result.incompleteRecords
    }

    suspend fun toggleFavorite(id: String) {
        if (dao.toggleFavorite(id) == 0) {
            throw IOException("Asteroid '$id' is no longer available.")
        }
    }
}

internal fun mapFeed(
    feed: NeoWsFeedDto,
    startDate: LocalDate,
    endDate: LocalDate,
): NeoWsFetchResult {
    val groupedObjects = feed.nearEarthObjects
        ?: throw IOException("NASA NeoWs response is missing near_earth_objects.")
    var incompleteRecords = 0
    val items = buildList {
        for ((date, asteroids) in groupedObjects) {
            val parsedDate = try {
                LocalDate.parse(date)
            } catch (_: DateTimeParseException) {
                incompleteRecords++
                continue
            }
            if (parsedDate < startDate || parsedDate > endDate) continue
            if (asteroids == null) {
                incompleteRecords++
                continue
            }
            for (dto in asteroids) {
                if (dto == null || dto.id.isNullOrBlank()) {
                    incompleteRecords++
                    continue
                }
                val approach = dto.closeApproaches
                    ?.firstOrNull { it?.date == date }
                val diameter = dto.estimatedDiameter?.meters
                val velocity = approach?.relativeVelocity?.kilometersPerHour
                    .toFiniteDoubleOrNull()
                val distance = approach?.missDistance?.kilometers.toFiniteDoubleOrNull()
                val diameterMin = diameter?.min.toFiniteDoubleOrNull()
                val diameterMax = diameter?.max.toFiniteDoubleOrNull()
                val name = dto.name?.takeUnless(String::isBlank)
                if (
                    name == null || diameterMin == null || diameterMax == null ||
                    velocity == null || distance == null ||
                    dto.isPotentiallyHazardous == null
                ) {
                    incompleteRecords++
                }
                add(
                    NearEarthObject(
                        id = dto.id,
                        name = name ?: "Asteroide sin nombre",
                        closeApproachDate = date,
                        estimatedDiameterMinMeters = diameterMin,
                        estimatedDiameterMaxMeters = diameterMax,
                        relativeVelocityKph = velocity,
                        missDistanceKm = distance,
                        isPotentiallyHazardous = dto.isPotentiallyHazardous,
                    ),
                )
            }
        }
    }.sortedByRecentApproach()
    return NeoWsFetchResult(items, incompleteRecords)
}

internal fun parseFeed(
    response: String,
    startDate: LocalDate,
    endDate: LocalDate,
): NeoWsFetchResult = mapFeed(
    Gson().fromJson(response, NeoWsFeedDto::class.java),
    startDate,
    endDate,
)

private fun String?.toFiniteDoubleOrNull(): Double? =
    this?.toDoubleOrNull()?.takeIf(Double::isFinite)

private fun com.google.gson.JsonElement?.toFiniteDoubleOrNull(): Double? =
    this?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asString
        ?.toDoubleOrNull()
        ?.takeIf(Double::isFinite)
