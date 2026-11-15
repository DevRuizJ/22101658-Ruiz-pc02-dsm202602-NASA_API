package dev.ruizj.appnasa.apod

import com.google.gson.annotations.SerializedName

data class ApodDto(
    val date: String?,
    val title: String?,
    val explanation: String?,
    @SerializedName("media_type")
    val mediaType: String?,
    val hdurl: String?,
    val credit: String?,
    val copyright: String?,
)

fun ApodDto.toDomainOrNull(): ApodItem? {
    val validDate = date?.takeIf { runCatching { java.time.LocalDate.parse(it) }.isSuccess }
        ?: return null
    val normalizedMediaType = mediaType?.takeIf { it == "image" || it == "video" } ?: "unknown"
    return ApodItem(
        date = validDate,
        title = title?.takeUnless(String::isBlank) ?: "Sin título",
        explanation = explanation?.takeUnless(String::isBlank) ?: "Descripción no disponible.",
        mediaType = normalizedMediaType,
        imageUrl = hdurl?.takeUnless(String::isBlank)?.takeIf { normalizedMediaType == "image" },
        credit = credit?.takeUnless(String::isBlank)
            ?: copyright?.takeUnless(String::isBlank),
    )
}
