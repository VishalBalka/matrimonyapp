package com.matrimonyapp.data.discovery

import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.ApiErrorMapper
import com.matrimonyapp.data.remote.ProfileDetailDto
import com.matrimonyapp.data.remote.ProfileSearchApi
import com.matrimonyapp.data.remote.ProfileSearchItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Discovery data access. Error mapping goes through ApiErrorMapper, and HTTP 401 is
 * reported as AppError.requiresAuthentication; clearing the stored token on 401 is
 * done once, centrally, by ApiClient's authentication interceptor.
 */
class ProfileSearchRepository(
    private val api: ProfileSearchApi
) {
    suspend fun search(
        filters: ProfileSearchFilters,
        page: Int,
        pageSize: Int = SEARCH_PAGE_SIZE
    ): AppResult<ProfileSearchPage> = withContext(Dispatchers.IO) {
        try {
            val response = api.search(
                country = filters.country?.trim()?.ifEmpty { null },
                state = filters.state?.trim()?.ifEmpty { null },
                city = filters.city?.trim()?.ifEmpty { null },
                minAge = filters.minAge,
                maxAge = filters.maxAge,
                page = page.coerceAtLeast(0),
                pageSize = pageSize.coerceIn(1, MAX_SEARCH_PAGE_SIZE)
            )
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return@withContext AppResult.Failure(emptyResponse("search"))
                AppResult.Success(
                    ProfileSearchPage(
                        items = body.items.map { it.toDomain() },
                        page = body.page,
                        pageSize = body.pageSize,
                        totalItems = body.totalItems,
                        totalPages = body.totalPages
                    )
                )
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun getProfile(profileId: String): AppResult<DiscoveryProfile> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProfile(profileId)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return@withContext AppResult.Failure(emptyResponse("profile"))
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun getPhotoBytes(profileId: String): AppResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPhoto(profileId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    AppResult.Failure(emptyResponse("photo"))
                } else {
                    AppResult.Success(bytes)
                }
            } else {
                AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    private fun emptyResponse(what: String) =
        AppError("EMPTY_RESPONSE", "The server returned an empty $what response.")

    private fun ProfileSearchItemDto.toDomain() = DiscoveryProfile(
        profileId = profileId,
        displayName = displayName,
        age = age,
        country = country,
        stateProvince = stateProvince,
        city = city,
        profession = profession,
        skills = skills,
        salaryRange = salaryRange,
        linkedinUrl = linkedinUrl,
        instagramUrl = instagramUrl,
        facebookUrl = facebookUrl,
        websiteUrl = websiteUrl,
        verified = verified,
        photoAvailable = photoAvailable,
        photoUrl = photoUrl
    )

    private fun ProfileDetailDto.toDomain() = DiscoveryProfile(
        profileId = profileId,
        displayName = displayName,
        age = age,
        country = country,
        stateProvince = stateProvince,
        city = city,
        profession = profession,
        skills = skills,
        salaryRange = salaryRange,
        linkedinUrl = linkedinUrl,
        instagramUrl = instagramUrl,
        facebookUrl = facebookUrl,
        websiteUrl = websiteUrl,
        verified = verified,
        photoAvailable = photoAvailable,
        photoUrl = photoUrl
    )
}
