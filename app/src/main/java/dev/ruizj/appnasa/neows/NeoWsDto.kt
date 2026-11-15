package dev.ruizj.appnasa.neows

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class NeoWsFeedDto(
    @SerializedName("near_earth_objects")
    val nearEarthObjects: Map<String, List<NearEarthObjectDto?>?>?,
)

data class NearEarthObjectDto(
    val id: String?,
    val name: String?,
    @SerializedName("is_potentially_hazardous_asteroid")
    val isPotentiallyHazardous: Boolean?,
    @SerializedName("estimated_diameter")
    val estimatedDiameter: EstimatedDiameterDto?,
    @SerializedName("close_approach_data")
    val closeApproaches: List<CloseApproachDto?>?,
)

data class EstimatedDiameterDto(val meters: DiameterMetersDto?)

data class DiameterMetersDto(
    @SerializedName("estimated_diameter_min")
    val min: JsonElement?,
    @SerializedName("estimated_diameter_max")
    val max: JsonElement?,
)

data class CloseApproachDto(
    @SerializedName("close_approach_date")
    val date: String?,
    @SerializedName("relative_velocity")
    val relativeVelocity: RelativeVelocityDto?,
    @SerializedName("miss_distance")
    val missDistance: MissDistanceDto?,
)

data class RelativeVelocityDto(
    @SerializedName("kilometers_per_hour")
    val kilometersPerHour: String?,
)

data class MissDistanceDto(val kilometers: String?)
