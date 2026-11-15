package dev.ruizj.appnasa.data

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.ruizj.appnasa.apod.ApodDao
import dev.ruizj.appnasa.apod.ApodEntity
import dev.ruizj.appnasa.neows.NeoWsDao
import dev.ruizj.appnasa.neows.NeoWsEntity

@Database(
    entities = [ApodEntity::class, NeoWsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
    abstract fun neoWsDao(): NeoWsDao
}
