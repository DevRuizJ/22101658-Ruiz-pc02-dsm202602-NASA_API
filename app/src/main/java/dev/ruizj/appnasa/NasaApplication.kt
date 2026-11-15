package dev.ruizj.appnasa

import android.app.Application
import androidx.room.Room
import dev.ruizj.appnasa.apod.ApodRepository
import dev.ruizj.appnasa.data.AppDatabase
import dev.ruizj.appnasa.data.NasaApiServices
import dev.ruizj.appnasa.neows.NeoWsRepository

class NasaApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var apodRepository: ApodRepository
        private set
    lateinit var neoWsRepository: NeoWsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "nasa.db",
        ).build()
        val apis = NasaApiServices()
        apodRepository = ApodRepository(apis.apod, database.apodDao())
        neoWsRepository = NeoWsRepository(apis.neoWs, database.neoWsDao())
    }
}
