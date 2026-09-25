package com.matrimonyapp.data.discovery

import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult

const val SEARCH_PAGE_SIZE = 20
const val MAX_SEARCH_PAGE_SIZE = 50
const val MIN_SEARCH_AGE = 18
const val MAX_SEARCH_AGE = 100

private const val MAX_COUNTRY_LENGTH = 100
private const val MAX_STATE_LENGTH = 100
private const val MAX_CITY_LENGTH = 120

/** Discovery-safe view of another member. Never carries phone, DOB or internal ids. */
data class DiscoveryProfile(
    val profileId: String,
    val displayName: String,
    val age: Int,
    val country: String?,
    val stateProvince: String?,
    val city: String?,
    val profession: String?,
    val skills: String?,
    val salaryRange: String?,
    val linkedinUrl: String?,
    val instagramUrl: String?,
    val facebookUrl: String?,
    val websiteUrl: String?,
    val verified: Boolean,
    val photoAvailable: Boolean,
    val photoUrl: String?
)

data class ProfileSearchFilters(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val minAge: Int? = null,
    val maxAge: Int? = null
)

data class ProfileSearchPage(
    val items: List<DiscoveryProfile>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
)

// ---------------------------------------------------------------------------
// Filter validation (mirrors the server rules; the server remains authoritative)
// ---------------------------------------------------------------------------

data class SearchFilterErrors(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val minAge: String? = null,
    val maxAge: String? = null
) {
    val hasErrors: Boolean
        get() = country != null || state != null || city != null || minAge != null || maxAge != null
}

sealed interface SearchFilterValidation {
    data class Valid(val filters: ProfileSearchFilters) : SearchFilterValidation
    data class Invalid(val errors: SearchFilterErrors) : SearchFilterValidation
}

fun validateSearchFilters(
    country: String,
    state: String,
    city: String,
    minAge: String,
    maxAge: String
): SearchFilterValidation {
    val countryValue = country.trim()
    val stateValue = state.trim()
    val cityValue = city.trim()

    val countryError = textError("Country", countryValue, MAX_COUNTRY_LENGTH)
    val stateError = textError("State or province", stateValue, MAX_STATE_LENGTH)
    val cityError = textError("City", cityValue, MAX_CITY_LENGTH)

    val min = parseAge("Minimum age", minAge)
    val max = parseAge("Maximum age", maxAge)

    var minError = min.error
    var maxError = max.error
    if (minError == null && maxError == null && min.value != null && max.value != null && min.value > max.value) {
        minError = "Minimum age cannot be greater than maximum age."
    }

    val errors = SearchFilterErrors(countryError, stateError, cityError, minError, maxError)
    if (errors.hasErrors) return SearchFilterValidation.Invalid(errors)

    return SearchFilterValidation.Valid(
        ProfileSearchFilters(
            country = countryValue.ifEmpty { null },
            state = stateValue.ifEmpty { null },
            city = cityValue.ifEmpty { null },
            minAge = min.value,
            maxAge = max.value
        )
    )
}

private fun textError(label: String, value: String, maxLength: Int): String? = when {
    value.length > maxLength -> "$label must be $maxLength characters or fewer."
    value.any { it.isISOControl() } -> "$label contains unsupported characters."
    else -> null
}

private class ParsedAge(val value: Int?, val error: String?)

private fun parseAge(label: String, raw: String): ParsedAge {
    val text = raw.trim()
    if (text.isEmpty()) return ParsedAge(null, null)
    if (text.length > 3 || !text.all { it in '0'..'9' }) {
        return ParsedAge(null, "$label must be a whole number.")
    }
    val age = text.toInt()
    if (age < MIN_SEARCH_AGE || age > MAX_SEARCH_AGE) {
        return ParsedAge(null, "$label must be between $MIN_SEARCH_AGE and $MAX_SEARCH_AGE.")
    }
    return ParsedAge(age, null)
}

// ---------------------------------------------------------------------------
// Search state and pagination
// ---------------------------------------------------------------------------

enum class SearchPhase { INITIAL, LOADING, RESULTS, EMPTY, ERROR }

/**
 * Immutable search state. Every transition returns a new instance.
 *
 * requestId guards against stale responses: a response is applied only when the
 * id it was started with is still the current one, so a slow earlier search can
 * never overwrite a newer search, a cleared form, or a later page.
 */
data class SearchState(
    val phase: SearchPhase = SearchPhase.INITIAL,
    val filters: ProfileSearchFilters = ProfileSearchFilters(),
    val items: List<DiscoveryProfile> = emptyList(),
    val page: Int = -1,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
    val loadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val requestId: Int = 0
) {
    val hasMore: Boolean
        get() = phase == SearchPhase.RESULTS && page + 1 < totalPages

    val canLoadMore: Boolean
        get() = hasMore && !loadingMore

    val nextPage: Int
        get() = page + 1

    fun beginSearch(newFilters: ProfileSearchFilters): SearchState = copy(
        phase = SearchPhase.LOADING,
        filters = newFilters,
        items = emptyList(),
        page = -1,
        totalItems = 0,
        totalPages = 0,
        loadingMore = false,
        errorMessage = null,
        loadMoreError = null,
        requestId = requestId + 1
    )

    fun applyFirstPage(forRequest: Int, result: AppResult<ProfileSearchPage>): SearchState {
        if (forRequest != requestId) return this
        return when (result) {
            is AppResult.Success -> {
                val page = result.value
                val unique = page.items.distinctBy { it.profileId }
                copy(
                    phase = if (unique.isEmpty()) SearchPhase.EMPTY else SearchPhase.RESULTS,
                    items = unique,
                    page = page.page,
                    totalItems = page.totalItems,
                    totalPages = page.totalPages,
                    errorMessage = null
                )
            }
            is AppResult.Failure -> copy(
                phase = SearchPhase.ERROR,
                items = emptyList(),
                errorMessage = result.error.message
            )
        }
    }

    fun beginLoadMore(): SearchState =
        if (!canLoadMore) this else copy(loadingMore = true, loadMoreError = null, requestId = requestId + 1)

    fun applyNextPage(forRequest: Int, result: AppResult<ProfileSearchPage>): SearchState {
        if (forRequest != requestId || !loadingMore) return this
        return when (result) {
            is AppResult.Success -> {
                val page = result.value
                // A profile inserted between page requests can shift rows; never show one twice.
                val merged = (items + page.items).distinctBy { it.profileId }
                copy(
                    items = merged,
                    page = page.page,
                    totalItems = page.totalItems,
                    totalPages = page.totalPages,
                    loadingMore = false,
                    loadMoreError = null
                )
            }
            is AppResult.Failure -> copy(loadingMore = false, loadMoreError = result.error.message)
        }
    }

    /** Clears results and invalidates any in-flight request. */
    fun reset(): SearchState = SearchState(requestId = requestId + 1)
}

fun AppError.isAuthenticationFailure(): Boolean =
    requiresAuthentication || code == "UNAUTHORIZED" || code == "AUTHENTICATION_REQUIRED"
