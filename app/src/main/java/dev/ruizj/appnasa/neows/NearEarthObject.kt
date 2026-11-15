package dev.ruizj.appnasa.neows

data class NearEarthObject(
    val id: String,
    val name: String,
    val closeApproachDate: String,
    val estimatedDiameterMinMeters: Double?,
    val estimatedDiameterMaxMeters: Double?,
    val relativeVelocityKph: Double?,
    val missDistanceKm: Double?,
    val isPotentiallyHazardous: Boolean?,
)

fun List<NearEarthObject>.filterHazardousOnly(enabled: Boolean): List<NearEarthObject> =
    if (enabled) filter { it.isPotentiallyHazardous == true } else this

fun List<NearEarthObject>.sortedByRecentApproach(): List<NearEarthObject> =
    sortedWith(
        compareByDescending<NearEarthObject> { it.closeApproachDate }
            .thenBy { it.id },
    )
