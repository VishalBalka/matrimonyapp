package com.matrimonyapp.data.security

import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.CodeRequest
import com.matrimonyapp.data.remote.MessageResponseDto
import com.matrimonyapp.data.remote.MfaSetupDto
import com.matrimonyapp.data.remote.PrivacyDto
import com.matrimonyapp.data.remote.ReportRequest
import com.matrimonyapp.data.remote.SecurityApi
import com.matrimonyapp.data.remote.SecurityStateDto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SecurityRepositoryTest {
    private class FakeApi : SecurityApi {
        var calls = 0
        var lastCode: CodeRequest? = null
        var lastPrivacy: PrivacyDto? = null
        var lastPath: String? = null
        var lastReport: ReportRequest? = null
        var next: () -> Response<MessageResponseDto> = { Response.success(MessageResponseDto("Done from server.")) }

        override suspend fun state(): Response<SecurityStateDto> {
            calls++
            return Response.success(SecurityStateDto(true, "PRIVATE", false, true, false, true))
        }

        override suspend fun setupMfa(): Response<MfaSetupDto> {
            calls++
            return Response.success(MfaSetupDto("JBSWY3DPEHPK3PXP", "Scan or enter the secret."))
        }

        override suspend fun enableMfa(request: CodeRequest): Response<MessageResponseDto> {
            calls++; lastCode = request; return next()
        }

        override suspend fun disableMfa(request: CodeRequest): Response<MessageResponseDto> {
            calls++; lastCode = request; return next()
        }

        override suspend fun privacy(request: PrivacyDto): Response<MessageResponseDto> {
            calls++; lastPrivacy = request; return next()
        }

        override suspend fun block(userId: String): Response<MessageResponseDto> {
            calls++; lastPath = "block:$userId"; return next()
        }

        override suspend fun unblock(userId: String): Response<MessageResponseDto> {
            calls++; lastPath = "unblock:$userId"; return next()
        }

        override suspend fun report(userId: String, request: ReportRequest): Response<MessageResponseDto> {
            calls++; lastPath = "report:$userId"; lastReport = request; return next()
        }
    }

    private val api = FakeApi()
    private val repository = SecurityRepository(api)

    @Test
    fun stateMapsEveryServerField() = runBlocking {
        val state = (repository.state() as AppResult.Success).value

        assertTrue(state.mfaEnabled)
        assertEquals("PRIVATE", state.visibility)
        assertFalse(state.profileLocked)
        assertTrue(state.showPhone)
        assertFalse(state.showSalary)
        assertTrue(state.showSocial)
    }

    @Test
    fun setupMfaReturnsTheSecretAndMessage() = runBlocking {
        val setup = (repository.setupMfa() as AppResult.Success).value

        assertEquals("JBSWY3DPEHPK3PXP", setup.secret)
        assertEquals("Scan or enter the secret.", setup.message)
    }

    @Test
    fun enableAndDisableSendTheTrimmedSixDigitCode() = runBlocking {
        val enabled = repository.enableMfa(" 123456 ")
        assertEquals(CodeRequest("123456"), api.lastCode)
        assertEquals("Done from server.", (enabled as AppResult.Success).value)

        api.lastCode = null
        repository.disableMfa("654321")
        assertEquals(CodeRequest("654321"), api.lastCode)
    }

    @Test
    fun malformedCodesAreRejectedWithoutCallingTheServer() = runBlocking {
        for (bad in listOf("", "12345", "1234567", "abcdef", "12 456", "12345a")) {
            assertTrue("enable '$bad'", repository.enableMfa(bad) is AppResult.Failure)
            assertTrue("disable '$bad'", repository.disableMfa(bad) is AppResult.Failure)
        }
        assertEquals(0, api.calls)
        assertEquals("VALIDATION_ERROR", (repository.enableMfa("x") as AppResult.Failure).error.code)
    }

    @Test
    fun privacyForwardsAllFiveSettings() = runBlocking {
        val request = PrivacyDto("LOCKED", true, false, true, false)

        repository.privacy(request)

        assertEquals(request, api.lastPrivacy)
    }

    @Test
    fun blockAndUnblockUseTheGivenUserId() = runBlocking {
        repository.block(" user-1 ")
        assertEquals("block:user-1", api.lastPath)
        repository.unblock("user-1")
        assertEquals("unblock:user-1", api.lastPath)
    }

    @Test
    fun blankTargetsAreRejectedWithoutCallingTheServer() = runBlocking {
        assertTrue(repository.block("  ") is AppResult.Failure)
        assertTrue(repository.unblock("") is AppResult.Failure)
        assertTrue(repository.report("", "Spam", null) is AppResult.Failure)
        assertEquals(0, api.calls)
    }

    @Test
    fun reportTrimsAndDropsBlankDetails() = runBlocking {
        repository.report("user-1", " Fake profile ", "   ")
        assertEquals("report:user-1", api.lastPath)
        assertEquals(ReportRequest("Fake profile", null), api.lastReport)

        repository.report("user-1", "Harassment", " Sent abusive messages ")
        assertEquals(ReportRequest("Harassment", "Sent abusive messages"), api.lastReport)
    }

    @Test
    fun reportRequiresAReason() = runBlocking {
        val result = repository.report("user-1", "  ", "details")

        assertEquals("VALIDATION_ERROR", (result as AppResult.Failure).error.code)
        assertEquals(0, api.calls)
    }

    @Test
    fun successfulResponseWithoutABodyStillSucceedsWithAFallbackMessage() = runBlocking {
        api.next = { Response.success<MessageResponseDto>(null) }

        val result = repository.block("user-1")

        assertEquals("User blocked.", (result as AppResult.Success).value)
    }

    @Test
    fun unauthorizedRequiresAuthentication() = runBlocking {
        api.next = { Response.error(401, """{"code":"UNAUTHORIZED","message":"x"}""".toResponseBody()) }

        val error = (repository.privacy(PrivacyDto("PUBLIC", false, false, false, true)) as AppResult.Failure).error

        assertTrue(error.requiresAuthentication)
    }

    @Test
    fun serverErrorUsesTheServerMessage() = runBlocking {
        api.next = {
            Response.error(400, """{"code":"INVALID_MFA_CODE","message":"Invalid authenticator code."}""".toResponseBody())
        }

        val error = (repository.enableMfa("123456") as AppResult.Failure).error

        assertEquals("INVALID_MFA_CODE", error.code)
        assertEquals("Invalid authenticator code.", error.message)
        assertFalse(error.requiresAuthentication)
    }

    @Test
    fun networkFailureIsMappedNotThrown() = runBlocking {
        api.next = { throw IOException("connection details must not leak") }

        val error = (repository.block("user-1") as AppResult.Failure).error

        assertEquals("NETWORK_ERROR", error.code)
        assertFalse(error.message.contains("connection details"))
        assertNull(api.lastCode)
    }
}
