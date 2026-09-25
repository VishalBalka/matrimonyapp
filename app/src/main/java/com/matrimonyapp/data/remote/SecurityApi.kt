package com.matrimonyapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class SecurityStateDto(
    val mfaEnabled: Boolean,
    val visibility: String,
    val profileLocked: Boolean,
    val showPhone: Boolean,
    val showSalary: Boolean,
    val showSocial: Boolean
)

data class PrivacyDto(
    val visibility: String,
    val profileLocked: Boolean,
    val showPhone: Boolean,
    val showSalary: Boolean,
    val showSocial: Boolean
)

data class MfaSetupDto(
    val secret: String,
    val message: String
)

data class CodeRequest(
    val code: String
)

data class ReportRequest(
    val reason: String,
    val details: String?
)

interface SecurityApi {
    @GET("api/v1/security")
    suspend fun state(): Response<SecurityStateDto>

    @POST("api/v1/security/mfa/setup")
    suspend fun setupMfa(): Response<MfaSetupDto>

    @POST("api/v1/security/mfa/enable")
    suspend fun enableMfa(@Body request: CodeRequest): Response<MessageResponseDto>

    @POST("api/v1/security/mfa/disable")
    suspend fun disableMfa(@Body request: CodeRequest): Response<MessageResponseDto>

    @PUT("api/v1/security/privacy")
    suspend fun privacy(@Body request: PrivacyDto): Response<MessageResponseDto>

    @POST("api/v1/security/block/{userId}")
    suspend fun block(@Path("userId") userId: String): Response<MessageResponseDto>

    @DELETE("api/v1/security/block/{userId}")
    suspend fun unblock(@Path("userId") userId: String): Response<MessageResponseDto>

    @POST("api/v1/security/report/{userId}")
    suspend fun report(
        @Path("userId") userId: String,
        @Body request: ReportRequest
    ): Response<MessageResponseDto>
}
