package com.matrimonyapp.data.profile

data class ProfileValidation(
    val isValid: Boolean,
    val errors: List<String>
)

fun validateProfile(
    displayName: String,
    dateOfBirth: String,
    gender: String,
    country: String,
    stateProvince: String,
    city: String,
    bio: String,
    phoneNumber: String
): ProfileValidation {
    val errors = mutableListOf<String>()

    if (displayName.trim().isEmpty() || displayName.trim().length > 100) {
        errors += "Display name is required and must be 100 characters or fewer."
    }
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dateOfBirth.trim())) {
        errors += "Date of birth must use YYYY-MM-DD."
    }
    if (gender.uppercase() !in setOf("MALE", "FEMALE", "OTHER")) {
        errors += "Select a valid gender."
    }
    if (country.trim().isEmpty() || country.trim().length > 100) {
        errors += "Country is required and must be 100 characters or fewer."
    }
    if (stateProvince.trim().isEmpty() || stateProvince.trim().length > 100) {
        errors += "State or province is required and must be 100 characters or fewer."
    }
    if (city.trim().isEmpty() || city.trim().length > 120) {
        errors += "City is required and must be 120 characters or fewer."
    }
    if (bio.length > 500) {
        errors += "Bio must be 500 characters or fewer."
    }
    if (phoneNumber.isNotBlank() && !Regex("^[0-9+() .-]{7,24}$").matches(phoneNumber.trim())) {
        errors += "Phone number contains unsupported characters or is too long."
    }

    return ProfileValidation(errors.isEmpty(), errors)
}

// Backward-compatible overload used by existing callers/tests during Phase 08 migration.
fun validateProfile(
    displayName: String,
    dateOfBirth: String,
    gender: String,
    city: String,
    bio: String
): ProfileValidation = validateProfile(
    displayName, dateOfBirth, gender, "", "", city, bio, ""
).let { result ->
    if (result.errors.any { it.startsWith("Country is required") || it.startsWith("State or province is required") }) {
        val filtered = result.errors.filterNot {
            it.startsWith("Country is required") || it.startsWith("State or province is required")
        }
        ProfileValidation(filtered.isEmpty(), filtered)
    } else result
}
