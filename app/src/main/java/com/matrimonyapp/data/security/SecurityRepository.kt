package com.matrimonyapp.data.security

import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.ApiErrorMapper
import com.matrimonyapp.data.remote.CodeRequest
import com.matrimonyapp.data.remote.MessageResponseDto
import com.matrimonyapp.data.remote.MfaSetupDto
import com.matrimonyapp.data.remote.PrivacyDto
import com.matrimonyapp.data.remote.ReportRequest
import com.matrimonyapp.data.remote.SecurityApi
import com.matrimonyapp.data.remote.SecurityStateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

/**
 * MFA, privacy, block and report calls. HTTP and transport errors are mapped by ApiErrorMapper;
 * HTTP 401 surfaces as AppError.requiresAuthentication and the stored token is cleared centrally
 * by ApiClient's authentication interceptor.
 */
class SecurityRepository(
    private val api: SecurityApi
) {
    suspend fun state(): AppResult<SecurityStateDto> =
        execute({ api.state() }) { body -> body?.let { AppResult.Success(it) } ?: emptyBody() }

    suspend fun setupMfa(): AppResult<MfaSetupDto> =
        execute({ api.setupMfa() }) { body -> body?.let { AppResult.Success(it) } ?: emptyBody() }

    suspend fun enableMfa(code: String): AppResult<String> {
        val trimmed = code.trim()
        if (!isSixDigitCode(trimmed)) return invalidCode()
        return message("Multi-factor authentication enabled.") { api.enableMfa(CodeRequest(trimmed)) }
    }

    suspend fun disableMfa(code: String): AppResult<String> {
        val trimmed = code.trim()
        if (!isSixDigitCode(trimmed)) return invalidCode()
        return message("Multi-factor authentication disabled.") { api.disableMfa(CodeRequest(trimmed)) }
    }

    suspend fun privacy(request: PrivacyDto): AppResult<String> =
        message("Privacy settings saved.") { api.privacy(request) }

    suspend fun block(userId: String): AppResult<String> {
        if (userId.isBlank()) return missingTarget()
        return message("User blocked.") { api.block(userId.trim()) }
    }

    suspend fun unblock(userId: String): AppResult<String> {
        if (userId.isBlank()) return missingTarget()
        return message("User unblocked.") { api.unblock(userId.trim()) }
    }

    suspend fun report(userId: String, reason: String, details: String?): AppResult<String> {
        if (userId.isBlank()) return missingTarget()
        if (reason.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "A reason is required to submit a report."))
        }
        return message("Report submitted.") {
            api.report(userId.trim(), ReportRequest(reason.trim(), details?.trim()?.ifBlank { null }))
        }
    }

    /** Endpoints that answer with {"message": "..."}; a successful empty body is still a success. */
    private suspend fun message(
        fallback: String,
        request: suspend () -> Response<MessageResponseDto>
    ): AppResult<String> = execute(request) { body ->
        AppResult.Success(body?.message?.takeIf { it.isNotBlank() } ?: fallback)
    }

    private suspend fun <T, R> execute(
        request: suspend () -> Response<T>,
        onSuccess: (T?) -> AppResult<R>
    ): AppResult<R> = withContext(Dispatchers.IO) {
        try {
            val response = request()
            if (response.isSuccessful) {
                onSuccess(response.body())
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    private fun isSixDigitCode(code: String) = code.length == 6 && code.all { it in '0'..'9' }

    private fun emptyBody() =
        AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty response."))

    private fun invalidCode() =
        AppResult.Failure(AppError("VALIDATION_ERROR", "Enter the 6-digit code from your authenticator app."))

    private fun missingTarget() =
        AppResult.Failure(AppError("VALIDATION_ERROR", "No member was selected."))
}
