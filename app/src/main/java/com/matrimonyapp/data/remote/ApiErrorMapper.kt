package com.matrimonyapp.data.remote

import com.google.gson.Gson
import com.matrimonyapp.core.result.AppError
import retrofit2.Response
import java.io.IOException

object ApiErrorMapper {
    private val gson = Gson()

    fun fromResponse(response: Response<*>): AppError {
        if (response.code() == 401) {
            return AppError(
                code = "UNAUTHORIZED",
                message = "Your session has expired. Please sign in again.",
                requiresAuthentication = true
            )
        }

        val serverError = runCatching {
            response.errorBody()?.string()?.let {
                gson.fromJson(it, ErrorResponseDto::class.java)
            }
        }.getOrNull()

        val code = serverError?.code
        val message = when {
            response.code() == 429 -> serverError?.message ?: "Too many attempts. Please try again later."
            response.code() == 409 -> serverError?.message ?: "An account with this email already exists."
            response.isSuccessful.not() && !serverError?.message.isNullOrBlank() -> serverError?.message ?: "The request could not be completed."
            else -> "The request could not be completed."
        }

        return AppError(code, message)
    }

    fun fromThrowable(throwable: Throwable): AppError {
        return when (throwable) {
            is IOException -> AppError(
                code = "NETWORK_ERROR",
                message = "Unable to reach the server. Check the backend and network connection."
            )
            else -> AppError(
                code = "CLIENT_ERROR",
                message = "Something went wrong. Please try again."
            )
        }
    }
}
