package com.matrimonyapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/v1/auth/mfa/verify")
    suspend fun verifyMfa(@Body request: MfaVerifyRequestDto): Response<LoginResponseDto>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<MeResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<MessageResponseDto>
}
