package com.matrimonyapp.data.remote

/**
 * Discovery-safe search DTOs. These intentionally contain no phone number,
 * date of birth, user id, storage key, or authentication data.
 */
data class ProfileSearchItemDto(
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

data class ProfileSearchResponseDto(
    val items: List<ProfileSearchItemDto>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
)

data class ProfileDetailDto(
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
