package com.matrimonyapp.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

data class ProfileResponseDto(
    val profileId: String,
    val userId: String,
    val displayName: String,
    val dateOfBirth: String,
    val gender: String,
    val country: String?,
    val stateProvince: String?,
    val city: String,
    val bio: String,
    val phoneNumber: String?,
    val photoAvailable: Boolean,
    val profession: String?,
    val employer: String?,
    val salaryRange: String?,
    val education: String?,
    val skills: String?,
    val linkedinUrl: String?,
    val instagramUrl: String?,
    val facebookUrl: String?,
    val websiteUrl: String?,
    val profileVisibility: String,
    val showPhone: Boolean,
    val showSalary: Boolean,
    val showSocial: Boolean,
    val backgroundCheckStatus: String,
    val verificationStatus: String,
    val verifiedAt: String?,
    val profileLocked: Boolean
)

// The backend request uses primitive booleans, so a missing JSON field would be read as false.
// These are therefore non-null and always serialized, never omitted.
data class ProfileUpdateRequestDto(
    val displayName: String,
    val dateOfBirth: String,
    val gender: String,
    val country: String,
    val stateProvince: String,
    val city: String,
    val bio: String,
    val phoneNumber: String?,
    val profession: String?,
    val employer: String?,
    val salaryRange: String?,
    val education: String?,
    val skills: String?,
    val linkedinUrl: String?,
    val instagramUrl: String?,
    val facebookUrl: String?,
    val websiteUrl: String?,
    val profileVisibility: String,
    val showPhone: Boolean,
    val showSalary: Boolean,
    val showSocial: Boolean,
    val profileLocked: Boolean
)

interface ProfileApi {
    @GET("api/v1/profile")
    suspend fun getProfile(): Response<ProfileResponseDto>

    @PUT("api/v1/profile")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequestDto
    ): Response<ProfileResponseDto>

    @Multipart
    @POST("api/v1/profile/photo")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part
    ): Response<ProfileResponseDto>

    @GET("api/v1/profile/photo")
    suspend fun getPhoto(): Response<ResponseBody>

    @DELETE("api/v1/profile/photo")
    suspend fun deletePhoto(): Response<ProfileResponseDto>
}
