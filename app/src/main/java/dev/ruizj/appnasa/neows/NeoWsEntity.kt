package dev.ruizj.appnasa.neows

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "near_earth_objects",
    primaryKeys = ["id", "closeApproachDate"],
)
data class NeoWsEntity(
    val id: String,
    val name: String,
    val closeApproachDate: String,
    val estimatedDiameterMinMeters: Double?,
    val estimatedDiameterMaxMeters: Double?,
    val relativeVelocityKph: Double?,
    val missDistanceKm: Double?,
    val isPotentiallyHazardous: Boolean?,
    val isFavorite: Boolean = false,
    val isInCurrentFeed: Boolean = false,
)

@Dao
abstract class NeoWsDao {
    @Query(
        "SELECT * FROM near_earth_objects WHERE isInCurrentFeed = 1 " +
            "ORDER BY closeApproachDate DESC, id ASC",
    )
    abstract fun observeAll(): Flow<List<NeoWsEntity>>

    @Query(
        "SELECT * FROM near_earth_objects WHERE isFavorite = 1 " +
            "ORDER BY closeApproachDate DESC, id ASC",
    )
    abstract fun observeFavorites(): Flow<List<NeoWsEntity>>

    @Query("SELECT * FROM near_earth_objects")
    abstract suspend fun getAll(): List<NeoWsEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfMissing(entity: NeoWsEntity)

    @Query(
        """
        UPDATE near_earth_objects SET name = :name,
            estimatedDiameterMinMeters = :diameterMin,
            estimatedDiameterMaxMeters = :diameterMax,
            relativeVelocityKph = :velocityKph, missDistanceKm = :distanceKm,
            isPotentiallyHazardous = :isHazardous, isInCurrentFeed = 1
        WHERE id = :id AND closeApproachDate = :date
        """,
    )
    protected abstract suspend fun updateApiFields(
        id: String,
        date: String,
        name: String,
        diameterMin: Double?,
        diameterMax: Double?,
        velocityKph: Double?,
        distanceKm: Double?,
        isHazardous: Boolean?,
    )

    @Transaction
    open suspend fun upsertFromApi(items: List<NearEarthObject>) {
        markFeedAsStale()
        for (item in items) {
            insertIfMissing(item.toEntity())
            updateApiFields(
                item.id,
                item.closeApproachDate,
                item.name,
                item.estimatedDiameterMinMeters,
                item.estimatedDiameterMaxMeters,
                item.relativeVelocityKph,
                item.missDistanceKm,
                item.isPotentiallyHazardous,
            )
        }
    }

    @Query("UPDATE near_earth_objects SET isInCurrentFeed = 0")
    protected abstract suspend fun markFeedAsStale()

    @Query(
        """
        UPDATE near_earth_objects
        SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END
        WHERE id = :id
        """,
    )
    abstract suspend fun toggleFavorite(id: String): Int
}

fun NeoWsEntity.toDomain() = NearEarthObject(
    id = id,
    name = name,
    closeApproachDate = closeApproachDate,
    estimatedDiameterMinMeters = estimatedDiameterMinMeters,
    estimatedDiameterMaxMeters = estimatedDiameterMaxMeters,
    relativeVelocityKph = relativeVelocityKph,
    missDistanceKm = missDistanceKm,
    isPotentiallyHazardous = isPotentiallyHazardous,
)

fun NearEarthObject.toEntity() = NeoWsEntity(
    id = id,
    name = name,
    closeApproachDate = closeApproachDate,
    estimatedDiameterMinMeters = estimatedDiameterMinMeters,
    estimatedDiameterMaxMeters = estimatedDiameterMaxMeters,
    relativeVelocityKph = relativeVelocityKph,
    missDistanceKm = missDistanceKm,
    isPotentiallyHazardous = isPotentiallyHazardous,
)
