package dev.ruizj.appnasa.apod

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteException

class ApodFavoritesDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                date TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                explanation TEXT NOT NULL,
                media_type TEXT NOT NULL,
                image_url TEXT,
                credit TEXT
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getFavorites(): List<ApodItem> = readableDatabase.query(
        TABLE_FAVORITES,
        arrayOf("date", "title", "explanation", "media_type", "image_url", "credit"),
        null,
        null,
        null,
        null,
        "date DESC",
    ).use { cursor ->
        buildList(cursor.count) {
            while (cursor.moveToNext()) {
                add(
                    ApodItem(
                        date = cursor.getString(0),
                        title = cursor.getString(1),
                        explanation = cursor.getString(2),
                        mediaType = cursor.getString(3),
                        imageUrl = cursor.getString(4),
                        credit = cursor.getString(5),
                    ),
                )
            }
        }
    }

    fun saveFavorite(item: ApodItem) {
        val values = ContentValues().apply {
            put("date", item.date)
            put("title", item.title)
            put("explanation", item.explanation)
            put("media_type", item.mediaType)
            put("image_url", item.imageUrl)
            put("credit", item.credit)
        }
        val result = writableDatabase.insertWithOnConflict(
            TABLE_FAVORITES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        if (result == -1L) {
            throw SQLiteException("Could not save APOD favorite.")
        }
    }

    fun removeFavorite(date: String) {
        writableDatabase.delete(TABLE_FAVORITES, "date = ?", arrayOf(date))
    }

    private companion object {
        const val DATABASE_NAME = "apod_favorites.db"
        const val DATABASE_VERSION = 1
        const val TABLE_FAVORITES = "favorites"
    }
}
