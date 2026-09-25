package com.matrimonyapp.ui.auth

private const val MAX_DISPLAY_NAME_LENGTH = 80
private const val MIN_PASSWORD_LENGTH = 10
private const val MAX_PASSWORD_LENGTH = 128

fun validateEmail(value: String): String? {
    val email = value.trim()
    return when {
        email.isEmpty() -> "Email is required."
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address."
        else -> null
    }
}

fun validateRegistration(
    displayName: String,
    email: String,
    password: String,
    confirmPassword: String
): RegistrationValidation {
    val errors = mutableListOf<String>()
    if (displayName.trim().isEmpty()) errors += "Full name is required."
    if (displayName.trim().length > MAX_DISPLAY_NAME_LENGTH) errors += "Full name must be 80 characters or fewer."
    validateEmail(email)?.let(errors::add)
    if (password.length < MIN_PASSWORD_LENGTH) errors += "Password must be at least 10 characters."
    if (password.length > MAX_PASSWORD_LENGTH) errors += "Password must be 128 characters or fewer."
    if (password != confirmPassword) errors += "Passwords do not match."
    return RegistrationValidation(errors.isEmpty(), errors)
}

data class RegistrationValidation(val isValid: Boolean, val errors: List<String>)

data class LoginValidation(val isValid: Boolean, val errors: List<String>)

fun validateLogin(
    email: String,
    password: String
): LoginValidation {
    val errors = mutableListOf<String>()
    validateEmail(email)?.let(errors::add)
    if (password.length < MIN_PASSWORD_LENGTH) {
        errors += "Password must be at least 10 characters."
    }
    if (password.length > MAX_PASSWORD_LENGTH) {
        errors += "Password must be 128 characters or fewer."
    }
    return LoginValidation(errors.isEmpty(), errors)
}
