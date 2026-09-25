package com.matrimonyapp.data.repository

import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.security.TokenStore
import com.matrimonyapp.data.remote.AuthApi
import com.matrimonyapp.data.remote.LoginRequestDto
import com.matrimonyapp.data.remote.LoginResponseDto
import com.matrimonyapp.data.remote.MeResponseDto
import com.matrimonyapp.data.remote.MessageResponseDto
import com.matrimonyapp.data.remote.MfaVerifyRequestDto
import com.matrimonyapp.data.remote.RegisterRequestDto
import com.matrimonyapp.data.remote.RegisterResponseDto
import com.matrimonyapp.data.session.AuthState
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest {
    @Test
    fun loginStoresTokenAndPublishesAuthenticatedState() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repository = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repository.login("user@example.com", "password")

        assertTrue(result is AppResult.Success)
        assertEquals("test-token", tokenStore.value)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun logoutClearsTokenAndPublishesUnauthenticatedState() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repository = AuthRepository(FakeAuthApi(), tokenStore)
        repository.login("user@example.com", "password")

        val result = repository.logout()

        assertTrue(result is AppResult.Success)
        assertEquals(null, tokenStore.value)
        assertTrue(repository.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun loginCarriesTheServerRoleIntoTheAuthenticatedUser() = runBlocking {
        val api = FakeAuthApi().apply { loginResult = { success(role = "ADMIN") } }
        val repository = AuthRepository(api, FakeTokenStore())

        val user = (repository.login("admin@example.com", "password") as AppResult.Success).value

        assertEquals("ADMIN", user.role)
    }

    @Test
    fun mfaChallengeDoesNotStoreATokenOrAuthenticate() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply {
            loginResult = { challenge(token = "", challengeId = "challenge-123") }
        }
        val repository = AuthRepository(api, tokenStore)

        val result = repository.login("user@example.com", "password")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(MFA_REQUIRED_CODE, error.code)
        assertEquals("challenge-123", error.message) // LoginScreen reads the challenge id from here
        assertNull(tokenStore.value)
        assertFalse(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun mfaChallengeIsNeverStoredEvenIfTheServerAlsoSentAToken() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply {
            loginResult = { challenge(token = "unexpected-token", challengeId = "challenge-123") }
        }
        val repository = AuthRepository(api, tokenStore)

        repository.login("user@example.com", "password")

        assertNull(tokenStore.value)
        assertFalse(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun mfaRequiredWithoutAChallengeIdIsAnInvalidResponse() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply { loginResult = { challenge(token = "", challengeId = null) } }
        val repository = AuthRepository(api, tokenStore)

        val error = (repository.login("user@example.com", "password") as AppResult.Failure).error

        assertEquals("EMPTY_RESPONSE", error.code)
        assertNull(tokenStore.value)
    }

    @Test
    fun verifyMfaStoresTheSessionTokenAndAuthenticates() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply { verifyResult = { success(token = "mfa-session-token", role = "ADMIN") } }
        val repository = AuthRepository(api, tokenStore)

        val result = repository.verifyMfa("challenge-123", "123456")

        assertTrue(result is AppResult.Success)
        assertEquals("mfa-session-token", tokenStore.value)
        val user = (result as AppResult.Success).value
        assertEquals("ADMIN", user.role)
        val state = repository.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("ADMIN", (state as AuthState.Authenticated).user.role)
    }

    @Test
    fun verifyMfaTrimsTheChallengeAndCode() = runBlocking {
        val api = FakeAuthApi()
        val repository = AuthRepository(api, FakeTokenStore())

        repository.verifyMfa("  challenge-123 ", " 123456 ")

        assertEquals(MfaVerifyRequestDto("challenge-123", "123456"), api.lastVerify)
    }

    @Test
    fun rejectedMfaCodeStoresNothingAndDoesNotAuthenticate() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply {
            verifyResult = { Response.error(401, """{"code":"INVALID_MFA_CODE","message":"x"}""".toResponseBody()) }
        }
        val repository = AuthRepository(api, tokenStore)

        val result = repository.verifyMfa("challenge-123", "000000")

        assertTrue(result is AppResult.Failure)
        assertNull(tokenStore.value)
        assertFalse(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun verifyMfaNeverLoopsBackIntoAnotherChallenge() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply { verifyResult = { challenge(token = "", challengeId = "next") } }
        val repository = AuthRepository(api, tokenStore)

        val result = repository.verifyMfa("challenge-123", "123456")

        assertTrue(result is AppResult.Failure)
        assertNull(tokenStore.value)
        assertFalse(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun verifyMfaWithAnEmptyTokenIsRejected() = runBlocking {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi().apply { verifyResult = { success(token = "") } }
        val repository = AuthRepository(api, tokenStore)

        val result = repository.verifyMfa("challenge-123", "123456")

        assertEquals("EMPTY_RESPONSE", (result as AppResult.Failure).error.code)
        assertNull(tokenStore.value)
    }

    @Test
    fun verifyMfaNetworkFailureIsMappedNotThrown() = runBlocking {
        val api = FakeAuthApi().apply { verifyResult = { throw IOException("boom") } }
        val repository = AuthRepository(api, FakeTokenStore())

        val error = (repository.verifyMfa("challenge-123", "123456") as AppResult.Failure).error

        assertEquals("NETWORK_ERROR", error.code)
    }

    @Test
    fun restoreSessionCarriesTheRole() = runBlocking {
        val tokenStore = FakeTokenStore().apply { value = "existing-token" }
        val api = FakeAuthApi().apply { meResult = { Response.success(MeResponseDto("user-id", "Admin", "a@example.com", "ADMIN", true)) } }
        val repository = AuthRepository(api, tokenStore)

        val user = (repository.restoreSession() as AppResult.Success).value

        assertEquals("ADMIN", user?.role)
    }

    private class FakeTokenStore : TokenStore {
        var value: String? = null
        override fun save(token: String) { value = token }
        override fun get(): String? = value
        override fun clear() { value = null }
    }

    private class FakeAuthApi : AuthApi {
        var loginResult: () -> Response<LoginResponseDto> = { success() }
        var verifyResult: () -> Response<LoginResponseDto> = { success() }
        var meResult: () -> Response<MeResponseDto> =
            { Response.success(MeResponseDto("user-id", "Test User", "user@example.com", "MEMBER", false)) }
        var lastVerify: MfaVerifyRequestDto? = null

        override suspend fun register(request: RegisterRequestDto): Response<RegisterResponseDto> =
            Response.success(RegisterResponseDto("user-id", "Account created."))

        override suspend fun login(request: LoginRequestDto): Response<LoginResponseDto> = loginResult()

        override suspend fun verifyMfa(request: MfaVerifyRequestDto): Response<LoginResponseDto> {
            lastVerify = request
            return verifyResult()
        }

        override suspend fun me(): Response<MeResponseDto> = meResult()

        override suspend fun logout(): Response<MessageResponseDto> =
            Response.success(MessageResponseDto("Logout successful."))
    }

    companion object {
        private fun success(token: String = "test-token", role: String = "MEMBER") = Response.success(
            LoginResponseDto(
                token = token,
                userId = "user-id",
                displayName = "Test User",
                email = "user@example.com",
                expiresAt = "2026-09-17T00:00:00Z",
                mfaRequired = false,
                mfaChallengeId = null,
                role = role
            )
        )

        private fun challenge(token: String, challengeId: String?) = Response.success(
            LoginResponseDto(
                token = token,
                userId = "user-id",
                displayName = "Test User",
                email = "user@example.com",
                expiresAt = "2026-09-17T00:00:00Z",
                mfaRequired = true,
                mfaChallengeId = challengeId,
                role = "MEMBER"
            )
        )
    }
}
