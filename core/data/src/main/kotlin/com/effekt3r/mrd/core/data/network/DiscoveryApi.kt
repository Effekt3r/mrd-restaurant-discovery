package com.effekt3r.mrd.core.data.network

import com.effekt3r.mrd.core.data.BuildConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * The mock API (spec §5.1 and §5.9). `no-cache` makes OkHttp revalidate its cached copy with the ETag on every call,
 * so unchanged data comes back as a 304. The bodies stay raw: the list is parsed only when it changed.
 */
internal interface DiscoveryApi {
    @Headers("Cache-Control: max-age=0")
    @GET(BuildConfig.RESTAURANTS_PATH)
    suspend fun restaurants(): Response<ResponseBody>

    @Headers("Cache-Control: max-age=0")
    @GET("v1/menus/{id}.json")
    suspend fun menu(@Path("id") restaurantId: String): Response<ResponseBody>
}
