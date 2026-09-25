package com.matrimonyapp.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileSearchApi {
    // Null query values are omitted by Retrofit, so unspecified filters never restrict the search.
    @GET("api/v1/profiles/search")
    suspend fun search(
        @Query("country") country: String?,
        @Query("state") state: String?,
        @Query("city") city: String?,
        @Query("minAge") minAge: Int?,
        @Query("maxAge") maxAge: Int?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ProfileSearchResponseDto>

    @GET("api/v1/profiles/{profileId}")
    suspend fun getProfile(@Path("profileId") profileId: String): Response<ProfileDetailDto>

    @GET("api/v1/profiles/{profileId}/photo")
    suspend fun getPhoto(@Path("profileId") profileId: String): Response<ResponseBody>
}
