package dev.ruizj.appnasa.data

import com.google.gson.JsonElement
import dev.ruizj.appnasa.BuildConfig
import dev.ruizj.appnasa.neows.NeoWsFeedDto
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ApodApi {
    @GET("wp-json/wp/v2/apod-basic")
    suspend fun getApod(
        @Query("api_key") apiKey: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): Response<JsonElement>
}

interface NeoWsApi {
    @GET("neo/rest/v1/feed")
    suspend fun getFeed(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("api_key") apiKey: String,
    ): Response<NeoWsFeedDto>
}

class NasaApiServices {
    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apod: ApodApi = retrofit(BuildConfig.APOD_BASE_URL).create(ApodApi::class.java)
    val neoWs: NeoWsApi = retrofit(BuildConfig.NEOWS_BASE_URL).create(NeoWsApi::class.java)
}
