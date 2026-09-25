package com.matrimonyapp.data.remote

data class RegisterRequestDto(
    val displayName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterResponseDto(
    val userId: String,
    val message: String
)

data class LoginResponseDto(
    val token: String,
    val userId: String,
    val displayName: String,
    val email: String,
    val expiresAt: String,
    val mfaRequired: Boolean,
    val mfaChallengeId: String?,
    val role: String
)

data class MfaVerifyRequestDto(
    val challengeId: String,
    val code: String
)

data class MeResponseDto(
    val userId: String,
    val displayName: String,
    val email: String,
    val role: String,
    val mfaEnabled: Boolean
)

data class MessageResponseDto(
    val message: String
)

data class ErrorResponseDto(
    val code: String,
    val message: String
)

data class AuthenticatedUser(
    val userId: String,
    val displayName: String,
    val email: String,
    val expiresAt: String,
    // Least-privilege default; the server value always overrides it.
    val role: String = "MEMBER"
)
