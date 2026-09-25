package com.matrimonyapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val readAt: String?,
    val createdAt: String
)

interface NotificationApi {
    @GET("api/v1/notifications")
    suspend fun list(): Response<List<NotificationDto>>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<MessageResponseDto>
}
