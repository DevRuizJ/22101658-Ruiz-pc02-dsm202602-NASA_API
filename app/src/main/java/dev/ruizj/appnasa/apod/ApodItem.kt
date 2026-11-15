package dev.ruizj.appnasa.apod

import java.time.LocalDate

data class ApodItem(
    val date: String,
    val title: String,
    val explanation: String,
    val mediaType: String,
    val imageUrl: String?,
    val credit: String?,
) {
    val isVideo: Boolean
        get() = mediaType == "video"
}

internal fun List<ApodItem>.filterApodRange(
    startDate: LocalDate,
    endDate: LocalDate,
): List<ApodItem> = filter { item ->
    val date = runCatching { LocalDate.parse(item.date) }.getOrNull()
    date != null && date in startDate..endDate
}.sortedByDescending(ApodItem::date)
