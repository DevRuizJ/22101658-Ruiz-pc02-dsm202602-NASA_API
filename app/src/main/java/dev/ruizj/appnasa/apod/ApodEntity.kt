package dev.ruizj.appnasa.apod

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "apod")
data class ApodEntity(
    @PrimaryKey val date: String,
    val title: String,
    val explanation: String,
    val mediaType: String,
    val imageUrl: String?,
    val credit: String?,
    val isFavorite: Boolean = false,
    val isInCurrentFeed: Boolean = false,
)

@Dao
abstract class ApodDao {
    @Query("SELECT * FROM apod WHERE isInCurrentFeed = 1 ORDER BY date DESC")
    abstract fun observeAll(): Flow<List<ApodEntity>>

    @Query("SELECT * FROM apod WHERE isFavorite = 1 ORDER BY date DESC")
    abstract fun observeFavorites(): Flow<List<ApodEntity>>

    @Query("SELECT * FROM apod")
    abstract suspend fun getAll(): List<ApodEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfMissing(entity: ApodEntity)

    @Query(
        """
        UPDATE apod SET title = :title, explanation = :explanation,
            mediaType = :mediaType, imageUrl = :imageUrl, credit = :credit,
            isInCurrentFeed = 1
        WHERE date = :date
        """,
    )
    protected abstract suspend fun updateApiFields(
        date: String,
        title: String,
        explanation: String,
        mediaType: String,
        imageUrl: String?,
        credit: String?,
    )

    @Transaction
    open suspend fun upsertFromApi(items: List<ApodItem>) {
        markFeedAsStale()
        for (item in items) {
            insertIfMissing(item.toEntity())
            updateApiFields(
                item.date,
                item.title,
                item.explanation,
                item.mediaType,
                item.imageUrl,
                item.credit,
            )
        }
    }

    @Query("UPDATE apod SET isInCurrentFeed = 0")
    protected abstract suspend fun markFeedAsStale()

    @Query(
        "UPDATE apod SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END " +
            "WHERE date = :date",
    )
    abstract suspend fun toggleFavorite(date: String): Int
}

fun ApodEntity.toDomain() = ApodItem(
    date = date,
    title = title,
    explanation = explanation,
    mediaType = mediaType,
    imageUrl = imageUrl,
    credit = credit,
)

fun ApodItem.toEntity() = ApodEntity(
    date = date,
    title = title,
    explanation = explanation,
    mediaType = mediaType,
    imageUrl = imageUrl,
    credit = credit,
)
