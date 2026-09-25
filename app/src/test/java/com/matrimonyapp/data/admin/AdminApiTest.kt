package com.matrimonyapp.data.admin

import com.matrimonyapp.data.remote.AdminApi
import com.matrimonyapp.data.remote.AdminDashboardDto
import com.matrimonyapp.data.remote.AdminReportDto
import com.matrimonyapp.data.remote.AdminService
import com.matrimonyapp.data.remote.MessageResponseDto
import com.matrimonyapp.data.remote.StatusRequest
import com.matrimonyapp.data.remote.VerifyRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AdminApiTest {
    private class FakeService : AdminService {
        var lastStatusFilter: String? = "unset"
        var lastCall: String? = null
        var lastStatus: StatusRequest? = null
        var lastVerify: VerifyRequest? = null

        override suspend fun dashboard() = Response.success(AdminDashboardDto(10, 8, 2, 3))

        override suspend fun reports(status: String?): Response<List<AdminReportDto>> {
            lastStatusFilter = status
            return Response.success(listOf(AdminReportDto("r1", "u1", "u2", "Spam", null, "OPEN", "2026-09-19T10:00:00Z")))
        }

        override suspend fun verify(profileId: String, request: VerifyRequest): Response<MessageResponseDto> {
            lastCall = "verify:$profileId"; lastVerify = request
            return Response.success(MessageResponseDto("ok"))
        }

        override suspend fun backgroundCheck(profileId: String, request: StatusRequest): Response<MessageResponseDto> {
            lastCall = "background:$profileId"; lastStatus = request
            return Response.success(MessageResponseDto("ok"))
        }

        override suspend fun reportStatus(reportId: String, request: StatusRequest): Response<MessageResponseDto> {
            lastCall = "report:$reportId"; lastStatus = request
            return Response.success(MessageResponseDto("ok"))
        }

        override suspend fun lockUser(userId: String): Response<MessageResponseDto> {
            lastCall = "lock:$userId"
            return Response.success(MessageResponseDto("ok"))
        }
    }

    private val service = FakeService()
    private val api = AdminApi(service)

    @Test
    fun dashboardReturnsTheRetrofitResponseTheScreenInspects() = runBlocking {
        val response = api.dashboard()

        assertTrue(response.isSuccessful)
        assertEquals(10, response.body()?.users)
        assertEquals(3, response.body()?.pendingVerification)
    }

    @Test
    fun reportsPassesTheOptionalStatusFilterThrough() = runBlocking {
        api.reports(null)
        assertNull(service.lastStatusFilter)

        api.reports("OPEN")
        assertEquals("OPEN", service.lastStatusFilter)
    }

    @Test
    fun reportStatusTurnsThePlainStringIntoTheJsonBodyTheBackendExpects() = runBlocking {
        api.reportStatus("r1", "RESOLVED")

        assertEquals("report:r1", service.lastCall)
        assertEquals(StatusRequest("RESOLVED"), service.lastStatus)
    }

    @Test
    fun verifyBackgroundCheckAndLockHitTheRightEndpoints() = runBlocking {
        api.verify("p1", true)
        assertEquals("verify:p1", service.lastCall)
        assertEquals(VerifyRequest(true), service.lastVerify)

        api.backgroundCheck("p1", "CLEAR")
        assertEquals("background:p1", service.lastCall)
        assertEquals(StatusRequest("CLEAR"), service.lastStatus)

        api.lockUser("u9")
        assertEquals("lock:u9", service.lastCall)
    }
}
