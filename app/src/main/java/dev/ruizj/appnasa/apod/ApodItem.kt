package dev.ruizj.appnasa.apod

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
