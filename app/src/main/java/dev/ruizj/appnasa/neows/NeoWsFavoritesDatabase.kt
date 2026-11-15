package dev.ruizj.appnasa.neows

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class NeoWsFavoritesDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createFavoritesTable(db, TABLE_FAVORITES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_FAVORITES RENAME TO $LEGACY_TABLE_FAVORITES")
            createFavoritesTable(db, TABLE_FAVORITES)
            db.execSQL(
                """
                INSERT INTO $TABLE_FAVORITES (
                    id, name, close_approach_date, diameter_min_meters,
                    diameter_max_meters, velocity_kph, miss_distance_km,
                    is_potentially_hazardous
                )
                SELECT
                    id, name, close_approach_date, diameter_min_meters,
                    diameter_max_meters, velocity_kph, miss_distance_km,
                    is_potentially_hazardous
                FROM $LEGACY_TABLE_FAVORITES
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE $LEGACY_TABLE_FAVORITES")
        }
    }

    private fun createFavoritesTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL(
            """
            CREATE TABLE $tableName (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                close_approach_date TEXT NOT NULL,
                diameter_min_meters REAL,
                diameter_max_meters REAL,
                velocity_kph REAL,
                miss_distance_km REAL,
                is_potentially_hazardous INTEGER
            )
            """.trimIndent(),
        )
    }

    fun getFavorites(): List<NearEarthObject> = readableDatabase.query(
        TABLE_FAVORITES,
        arrayOf(
            "id",
            "name",
            "close_approach_date",
            "diameter_min_meters",
            "diameter_max_meters",
            "velocity_kph",
            "miss_distance_km",
            "is_potentially_hazardous",
        ),
        null,
        null,
        null,
        null,
        "close_approach_date DESC, id ASC",
    ).use { cursor ->
        buildList(cursor.count) {
            while (cursor.moveToNext()) {
                add(
                    NearEarthObject(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        closeApproachDate = cursor.getString(2),
                        estimatedDiameterMinMeters = cursor.nullableDouble(3),
                        estimatedDiameterMaxMeters = cursor.nullableDouble(4),
                        relativeVelocityKph = cursor.nullableDouble(5),
                        missDistanceKm = cursor.nullableDouble(6),
                        isPotentiallyHazardous = if (cursor.isNull(7)) {
                            null
                        } else {
                            cursor.getInt(7) != 0
                        },
                    ),
                )
            }
        }
    }

    fun saveFavorite(asteroid: NearEarthObject) {
        val values = ContentValues().apply {
            put("id", asteroid.id)
            put("name", asteroid.name)
            put("close_approach_date", asteroid.closeApproachDate)
            putNullableDouble("diameter_min_meters", asteroid.estimatedDiameterMinMeters)
            putNullableDouble("diameter_max_meters", asteroid.estimatedDiameterMaxMeters)
            putNullableDouble("velocity_kph", asteroid.relativeVelocityKph)
            putNullableDouble("miss_distance_km", asteroid.missDistanceKm)
            asteroid.isPotentiallyHazardous?.let {
                put("is_potentially_hazardous", if (it) 1 else 0)
            } ?: putNull("is_potentially_hazardous")
        }
        val result = writableDatabase.insertWithOnConflict(
            TABLE_FAVORITES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        if (result == -1L) {
            throw SQLiteException("Could not save asteroid favorite.")
        }
    }

    fun removeFavorite(id: String) {
        writableDatabase.delete(TABLE_FAVORITES, "id = ?", arrayOf(id))
    }

    private companion object {
        const val DATABASE_NAME = "neows_favorites.db"
        const val DATABASE_VERSION = 2
        const val TABLE_FAVORITES = "favorites"
        const val LEGACY_TABLE_FAVORITES = "favorites_legacy"
    }
}

private fun android.database.Cursor.nullableDouble(index: Int): Double? =
    if (isNull(index)) null else getDouble(index)

private fun ContentValues.putNullableDouble(key: String, value: Double?) {
    if (value == null) putNull(key) else put(key, value)
}
