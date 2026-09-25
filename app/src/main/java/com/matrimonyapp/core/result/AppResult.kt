package com.matrimonyapp.core.result

data class AppError(
    val code: String?,
    val message: String,
    val requiresAuthentication: Boolean = false
)

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
