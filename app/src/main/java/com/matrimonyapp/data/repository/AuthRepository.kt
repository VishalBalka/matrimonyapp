package com.matrimonyapp.data.repository

import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.security.TokenStore
import com.matrimonyapp.data.remote.ApiErrorMapper
import com.matrimonyapp.data.remote.AuthApi
import com.matrimonyapp.data.remote.AuthenticatedUser
import com.matrimonyapp.data.remote.LoginRequestDto
import com.matrimonyapp.data.remote.LoginResponseDto
import com.matrimonyapp.data.remote.MfaVerifyRequestDto
import com.matrimonyapp.data.remote.RegisterRequestDto
import com.matrimonyapp.data.session.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Error code LoginScreen uses to open the MFA verification step. */
const val MFA_REQUIRED_CODE = "MFA_REQUIRED"

class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState

    suspend fun restoreSession(): AppResult<AuthenticatedUser?> = withContext(Dispatchers.IO) {
        val token = tokenStore.get()
        if (token.isNullOrBlank()) {
            _authState.value = AuthState.Unauthenticated
            return@withContext AppResult.Success(null)
        }

        try {
            val response = api.me()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val user = AuthenticatedUser(
                        userId = body.userId,
                        displayName = body.displayName,
                        email = body.email,
                        expiresAt = "",
                        role = body.role
                    )
                    _authState.value = AuthState.Authenticated(user)
                    AppResult.Success(user)
                } else {
                    tokenStore.clear()
                    _authState.value = AuthState.Unauthenticated
                    AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty response."))
                }
            } else {
                tokenStore.clear()
                _authState.value = AuthState.Unauthenticated
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (throwable: Throwable) {
            tokenStore.clear()
            _authState.value = AuthState.Unauthenticated
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun register(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(
                RegisterRequestDto(
                    displayName = displayName.trim(),
                    email = email.trim(),
                    password = password,
                    confirmPassword = confirmPassword
                )
            )
            if (response.isSuccessful) {
                AppResult.Success(response.body()?.message ?: "Account created.")
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun login(email: String, password: String): AppResult<AuthenticatedUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(
                    LoginRequestDto(email = email.trim(), password = password)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return@withContext AppResult.Failure(
                            AppError("EMPTY_RESPONSE", "The server returned an invalid login response.")
                        )

                    if (body.mfaRequired) {
                        // Second factor pending: no session exists yet, so nothing is stored and
                        // the user is not authenticated. LoginScreen routes on this code and
                        // treats the message as the challenge id.
                        val challengeId = body.mfaChallengeId
                        return@withContext if (challengeId.isNullOrBlank()) {
                            AppResult.Failure(
                                AppError("EMPTY_RESPONSE", "The server returned an invalid login response.")
                            )
                        } else {
                            AppResult.Failure(AppError(MFA_REQUIRED_CODE, challengeId))
                        }
                    }

                    completeLogin(body)
                } else {
                    AppResult.Failure(ApiErrorMapper.fromResponse(response))
                }
            } catch (throwable: Throwable) {
                AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
            }
        }

    suspend fun verifyMfa(challengeId: String, code: String): AppResult<AuthenticatedUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.verifyMfa(
                    MfaVerifyRequestDto(challengeId = challengeId.trim(), code = code.trim())
                )

                if (response.isSuccessful) {
                    val body = response.body()
                        ?: return@withContext AppResult.Failure(
                            AppError("EMPTY_RESPONSE", "The server returned an invalid login response.")
                        )

                    // A verified challenge must yield a session; never loop back into another challenge.
                    if (body.mfaRequired) {
                        return@withContext AppResult.Failure(
                            AppError("MFA_FAILED", "Verification could not be completed. Please sign in again.")
                        )
                    }

                    completeLogin(body)
                } else {
                    AppResult.Failure(ApiErrorMapper.fromResponse(response))
                }
            } catch (throwable: Throwable) {
                AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
            }
        }

    /** Saves the session token and marks the user authenticated. Only called with a real session. */
    private fun completeLogin(body: LoginResponseDto): AppResult<AuthenticatedUser> {
        if (body.token.isBlank()) {
            return AppResult.Failure(
                AppError("EMPTY_RESPONSE", "The server returned an invalid login response.")
            )
        }

        tokenStore.save(body.token)
        val user = AuthenticatedUser(
            userId = body.userId,
            displayName = body.displayName,
            email = body.email,
            expiresAt = body.expiresAt,
            role = body.role
        )

        _authState.value = AuthState.Authenticated(user)
        return AppResult.Success(user)
    }

    suspend fun logout(): AppResult<String> = withContext(Dispatchers.IO) {
        val token = tokenStore.get()
        if (token.isNullOrBlank()) {
            _authState.value = AuthState.Unauthenticated
            return@withContext AppResult.Success("Signed out on this device.")
        }

        try {
            val response = api.logout()
            tokenStore.clear()
            _authState.value = AuthState.Unauthenticated

            if (response.isSuccessful) {
                AppResult.Success(response.body()?.message ?: "Logout successful.")
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (throwable: Throwable) {
            tokenStore.clear()
            _authState.value = AuthState.Unauthenticated
            AppResult.Failure(
                AppError(
                    code = "LOGOUT_UNCONFIRMED",
                    message = "You are signed out on this device, but server logout could not be confirmed."
                )
            )
        }
    }
}
