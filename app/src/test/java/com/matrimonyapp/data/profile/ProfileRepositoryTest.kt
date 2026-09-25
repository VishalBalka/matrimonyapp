package com.matrimonyapp.data.profile

import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.ProfileApi
import com.matrimonyapp.data.remote.ProfileResponseDto
import com.matrimonyapp.data.remote.ProfileUpdateRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProfileRepositoryTest {
    private class FakeApi : ProfileApi {
        var lastUpdate: ProfileUpdateRequestDto? = null
        var updateResult: () -> Response<ProfileResponseDto> = { Response.success(serverProfile()) }

        override suspend fun getProfile(): Response<ProfileResponseDto> = Response.success(serverProfile())

        override suspend fun updateProfile(request: ProfileUpdateRequestDto): Response<ProfileResponseDto> {
            lastUpdate = request
            return updateResult()
        }

        override suspend fun uploadPhoto(photo: MultipartBody.Part): Response<ProfileResponseDto> =
            Response.success(serverProfile())

        override suspend fun getPhoto(): Response<ResponseBody> = Response.success("x".toResponseBody(null))

        override suspend fun deletePhoto(): Response<ProfileResponseDto> = Response.success(serverProfile())
    }

    private val api = FakeApi()
    private val repository = ProfileRepository(api)

    private suspend fun update(
        profession: String = "", employer: String = "", salaryRange: String = "", education: String = "",
        skills: String = "", linkedin: String = "", instagram: String = "", facebook: String = "",
        website: String = "", visibility: String = "public", showPhone: Boolean = false,
        showSalary: Boolean = false, showSocial: Boolean = true, locked: Boolean = false
    ) = repository.updateProfile(
        " Asha ", " 1995-05-20 ", " female ", " India ", " Telangana ", " Hyderabad ", " Hi ", " +91 90000 11111 ",
        profession, employer, salaryRange, education, skills, linkedin, instagram, facebook, website,
        visibility, showPhone, showSalary, showSocial, locked
    )

    @Test
    fun updateTrimsRequiredFieldsAndNormalizesEnums() = runBlocking {
        update()

        val sent = api.lastUpdate!!
        assertEquals("Asha", sent.displayName)
        assertEquals("1995-05-20", sent.dateOfBirth)
        assertEquals("FEMALE", sent.gender)
        assertEquals("India", sent.country)
        assertEquals("Telangana", sent.stateProvince)
        assertEquals("Hyderabad", sent.city)
        assertEquals("Hi", sent.bio)
        assertEquals("+91 90000 11111", sent.phoneNumber)
        assertEquals("PUBLIC", sent.profileVisibility)
    }

    @Test
    fun blankOptionalFieldsAreSentAsNull() = runBlocking {
        update(profession = "  ", linkedin = "", website = " ")

        val sent = api.lastUpdate!!
        assertNull(sent.profession); assertNull(sent.employer); assertNull(sent.salaryRange)
        assertNull(sent.education); assertNull(sent.skills)
        assertNull(sent.linkedinUrl); assertNull(sent.instagramUrl); assertNull(sent.facebookUrl); assertNull(sent.websiteUrl)
    }

    @Test
    fun suppliedOptionalFieldsAreSentTrimmed() = runBlocking {
        update(
            profession = " Engineer ", employer = " Acme ", salaryRange = " 10-15 LPA ", education = " B.Tech ",
            skills = " Kotlin, Java ", linkedin = " https://www.linkedin.com/in/asha ",
            instagram = "https://www.instagram.com/asha", facebook = "https://www.facebook.com/asha",
            website = "https://asha.example.com"
        )

        val sent = api.lastUpdate!!
        assertEquals("Engineer", sent.profession)
        assertEquals("Acme", sent.employer)
        assertEquals("10-15 LPA", sent.salaryRange)
        assertEquals("B.Tech", sent.education)
        assertEquals("Kotlin, Java", sent.skills)
        assertEquals("https://www.linkedin.com/in/asha", sent.linkedinUrl)
        assertEquals("https://asha.example.com", sent.websiteUrl)
    }

    @Test
    fun privacyBooleansAreForwardedExactlyAsGiven() = runBlocking {
        update(showPhone = true, showSalary = true, showSocial = false, locked = true)

        val sent = api.lastUpdate!!
        assertTrue(sent.showPhone); assertTrue(sent.showSalary); assertFalse(sent.showSocial); assertTrue(sent.profileLocked)
    }

    @Test
    fun responseMapsEveryBackendField() = runBlocking {
        val profile = (update() as AppResult.Success).value

        assertEquals("profile-1", profile.profileId)
        assertEquals("user-1", profile.userId)
        assertEquals("Engineer", profile.profession)
        assertEquals("Acme", profile.employer)
        assertEquals("10-15 LPA", profile.salaryRange)
        assertEquals("B.Tech", profile.education)
        assertEquals("Kotlin", profile.skills)
        assertEquals("https://www.linkedin.com/in/asha", profile.linkedinUrl)
        assertEquals("PRIVATE", profile.profileVisibility)
        assertTrue(profile.showPhone)
        assertFalse(profile.showSalary)
        assertTrue(profile.showSocial)
        assertEquals("CLEAR", profile.backgroundCheckStatus)
        assertEquals("VERIFIED", profile.verificationStatus)
        assertEquals("2026-09-01T00:00:00Z", profile.verifiedAt)
        assertTrue(profile.profileLocked)
    }

    @Test
    fun serverErrorIsMappedThroughApiErrorMapper() = runBlocking {
        api.updateResult = {
            Response.error(400, """{"code":"PROFILE_VALIDATION_ERROR","message":"LinkedIn URL must use HTTPS."}""".toResponseBody())
        }

        val error = (update() as AppResult.Failure).error

        assertEquals("PROFILE_VALIDATION_ERROR", error.code)
        assertEquals("LinkedIn URL must use HTTPS.", error.message)
    }

    companion object {
        private fun serverProfile() = ProfileResponseDto(
            profileId = "profile-1", userId = "user-1", displayName = "Asha", dateOfBirth = "1995-05-20",
            gender = "FEMALE", country = "India", stateProvince = "Telangana", city = "Hyderabad", bio = "Hi",
            phoneNumber = "+91 90000 11111", photoAvailable = false, profession = "Engineer", employer = "Acme",
            salaryRange = "10-15 LPA", education = "B.Tech", skills = "Kotlin",
            linkedinUrl = "https://www.linkedin.com/in/asha", instagramUrl = null, facebookUrl = null, websiteUrl = null,
            profileVisibility = "PRIVATE", showPhone = true, showSalary = false, showSocial = true,
            backgroundCheckStatus = "CLEAR", verificationStatus = "VERIFIED", verifiedAt = "2026-09-01T00:00:00Z",
            profileLocked = true
        )
    }
}
