package com.matrimonyapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class AdminDashboardDto(
    val users: Int,
    val profiles: Int,
    val openReports: Int,
    val pendingVerification: Int
)

data class AdminReportDto(
    val id: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val reason: String,
    val details: String?,
    val status: String,
    val createdAt: String
)

data class VerifyRequest(val verified: Boolean)

data class StatusRequest(val status: String)

/** Raw Retrofit endpoints. Use AdminApi from application code. */
interface AdminService {
    @GET("api/v1/admin/dashboard")
    suspend fun dashboard(): Response<AdminDashboardDto>

    @GET("api/v1/admin/reports")
    suspend fun reports(@Query("status") status: String?): Response<List<AdminReportDto>>

    @POST("api/v1/admin/profiles/{profileId}/verify")
    suspend fun verify(
        @Path("profileId") profileId: String,
        @Body request: VerifyRequest
    ): Response<MessageResponseDto>

    @POST("api/v1/admin/profiles/{profileId}/background-check")
    suspend fun backgroundCheck(
        @Path("profileId") profileId: String,
        @Body request: StatusRequest
    ): Response<MessageResponseDto>

    @POST("api/v1/admin/reports/{reportId}/status")
    suspend fun reportStatus(
        @Path("reportId") reportId: String,
        @Body request: StatusRequest
    ): Response<MessageResponseDto>

    @POST("api/v1/admin/users/{userId}/lock")
    suspend fun lockUser(@Path("userId") userId: String): Response<MessageResponseDto>
}

/**
 * Thin wrapper over AdminService. AdminScreen calls it directly and passes plain strings
 * (for example reportStatus(id, "RESOLVED")); a Kotlin interface method with a body is not a
 * safe Retrofit endpoint, so the string-to-request-body convenience lives here instead.
 */
class AdminApi(private val service: AdminService) {
    suspend fun dashboard(): Response<AdminDashboardDto> = service.dashboard()

    suspend fun reports(status: String?): Response<List<AdminReportDto>> = service.reports(status)

    suspend fun verify(profileId: String, verified: Boolean): Response<MessageResponseDto> =
        service.verify(profileId, VerifyRequest(verified))

    suspend fun backgroundCheck(profileId: String, status: String): Response<MessageResponseDto> =
        service.backgroundCheck(profileId, StatusRequest(status))

    suspend fun reportStatus(reportId: String, status: String): Response<MessageResponseDto> =
        service.reportStatus(reportId, StatusRequest(status))

    suspend fun lockUser(userId: String): Response<MessageResponseDto> = service.lockUser(userId)
}
