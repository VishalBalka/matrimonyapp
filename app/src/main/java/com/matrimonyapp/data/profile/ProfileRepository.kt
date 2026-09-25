package com.matrimonyapp.data.profile

import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.ApiErrorMapper
import com.matrimonyapp.data.remote.ProfileApi
import com.matrimonyapp.data.remote.ProfileResponseDto
import com.matrimonyapp.data.remote.ProfileUpdateRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepository(
    private val api: ProfileApi
) {
    suspend fun getProfile(): AppResult<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProfile()
            when {
                response.isSuccessful -> response.body()?.let { AppResult.Success(it.toDomain()) }
                    ?: AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty profile response."))
                response.code() == 404 -> AppResult.Success(null)
                else -> AppResult.Failure(ApiErrorMapper.fromResponse(response))
            }
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun updateProfile(
        displayName: String, dateOfBirth: String, gender: String, country: String,
        stateProvince: String, city: String, bio: String, phoneNumber: String,
        profession: String, employer: String, salaryRange: String, education: String, skills: String,
        linkedinUrl: String, instagramUrl: String, facebookUrl: String, websiteUrl: String,
        profileVisibility: String, showPhone: Boolean, showSalary: Boolean, showSocial: Boolean,
        profileLocked: Boolean
    ): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateProfile(
                ProfileUpdateRequestDto(
                    displayName = displayName.trim(),
                    dateOfBirth = dateOfBirth.trim(),
                    gender = gender.trim().uppercase(),
                    country = country.trim(),
                    stateProvince = stateProvince.trim(),
                    city = city.trim(),
                    bio = bio.trim(),
                    phoneNumber = phoneNumber.trim().ifBlank { null },
                    profession = profession.trim().ifBlank { null },
                    employer = employer.trim().ifBlank { null },
                    salaryRange = salaryRange.trim().ifBlank { null },
                    education = education.trim().ifBlank { null },
                    skills = skills.trim().ifBlank { null },
                    linkedinUrl = linkedinUrl.trim().ifBlank { null },
                    instagramUrl = instagramUrl.trim().ifBlank { null },
                    facebookUrl = facebookUrl.trim().ifBlank { null },
                    websiteUrl = websiteUrl.trim().ifBlank { null },
                    profileVisibility = profileVisibility.trim().uppercase(),
                    showPhone = showPhone,
                    showSalary = showSalary,
                    showSocial = showSocial,
                    profileLocked = profileLocked
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { AppResult.Success(it.toDomain()) }
                    ?: AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty profile response."))
            } else AppResult.Failure(ApiErrorMapper.fromResponse(response))
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun uploadPhoto(bytes: ByteArray, mimeType: String): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val mediaType = mimeType.toMediaType()
            val requestBody = bytes.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("photo", "profile-photo", requestBody)
            val response = api.uploadPhoto(part)
            if (response.isSuccessful) {
                response.body()?.let { AppResult.Success(it.toDomain()) }
                    ?: AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty profile response."))
            } else AppResult.Failure(ApiErrorMapper.fromResponse(response))
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun getPhotoBytes(): AppResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPhoto()
            if (response.isSuccessful) {
                response.body()?.bytes()?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty photo."))
            } else AppResult.Failure(ApiErrorMapper.fromResponse(response))
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    suspend fun deletePhoto(): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = api.deletePhoto()
            if (response.isSuccessful) {
                response.body()?.let { AppResult.Success(it.toDomain()) }
                    ?: AppResult.Failure(AppError("EMPTY_RESPONSE", "The server returned an empty profile response."))
            } else AppResult.Failure(ApiErrorMapper.fromResponse(response))
        } catch (throwable: Throwable) {
            AppResult.Failure(ApiErrorMapper.fromThrowable(throwable))
        }
    }

    private fun ProfileResponseDto.toDomain() = UserProfile(
        profileId = profileId,
        userId = userId,
        displayName = displayName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        country = country,
        stateProvince = stateProvince,
        city = city,
        bio = bio,
        phoneNumber = phoneNumber,
        photoAvailable = photoAvailable,
        profession = profession,
        employer = employer,
        salaryRange = salaryRange,
        education = education,
        skills = skills,
        linkedinUrl = linkedinUrl,
        instagramUrl = instagramUrl,
        facebookUrl = facebookUrl,
        websiteUrl = websiteUrl,
        profileVisibility = profileVisibility,
        showPhone = showPhone,
        showSalary = showSalary,
        showSocial = showSocial,
        backgroundCheckStatus = backgroundCheckStatus,
        verificationStatus = verificationStatus,
        verifiedAt = verifiedAt,
        profileLocked = profileLocked
    )
}
