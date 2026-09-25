package com.matrimonyapp.data.discovery

import com.google.gson.Gson
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.ProfileDetailDto
import com.matrimonyapp.data.remote.ProfileSearchApi
import com.matrimonyapp.data.remote.ProfileSearchItemDto
import com.matrimonyapp.data.remote.ProfileSearchResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ProfileSearchRepositoryTest {
    private class SearchCall(
        val country: String?, val state: String?, val city: String?,
        val minAge: Int?, val maxAge: Int?, val page: Int, val pageSize: Int
    )

    private class FakeApi : ProfileSearchApi {
        var searchResult: () -> Response<ProfileSearchResponseDto> = { Response.success(page(emptyList())) }
        var profileResult: () -> Response<ProfileDetailDto> = { Response.success(detail()) }
        var photoResult: () -> Response<ResponseBody> = { Response.success("bytes".toResponseBody(null)) }
        var lastSearch: SearchCall? = null
        var lastProfileId: String? = null

        override suspend fun search(
            country: String?, state: String?, city: String?, minAge: Int?, maxAge: Int?, page: Int, pageSize: Int
        ): Response<ProfileSearchResponseDto> {
            lastSearch = SearchCall(country, state, city, minAge, maxAge, page, pageSize)
            return searchResult()
        }

        override suspend fun getProfile(profileId: String): Response<ProfileDetailDto> {
            lastProfileId = profileId
            return profileResult()
        }

        override suspend fun getPhoto(profileId: String): Response<ResponseBody> {
            lastProfileId = profileId
            return photoResult()
        }
    }

    private val api = FakeApi()
    private val repository = ProfileSearchRepository(api)

    @Test
    fun successMapsItemsAndPagination() = runBlocking {
        api.searchResult = { Response.success(page(listOf(item("a", "Asha"), item("b", "Bina")), total = 45, pages = 3)) }

        val result = repository.search(ProfileSearchFilters(), page = 0)

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).value
        assertEquals(listOf("Asha", "Bina"), page.items.map { it.displayName })
        assertEquals("a", page.items[0].profileId)
        assertEquals("Engineer", page.items[0].profession)
        assertEquals("10-15 LPA", page.items[0].salaryRange)
        assertEquals("https://www.linkedin.com/in/x", page.items[0].linkedinUrl)
        assertTrue(page.items[0].verified)
        assertEquals(45L, page.totalItems)
        assertEquals(3, page.totalPages)
    }

    @Test
    fun emptyResultsAreASuccessNotAnError() = runBlocking {
        api.searchResult = { Response.success(page(emptyList(), total = 0, pages = 0)) }

        val result = repository.search(ProfileSearchFilters(country = "Nowhere"), page = 0)

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).value.items.isEmpty())
    }

    @Test
    fun filtersAreSentAndBlankFiltersAreOmitted() = runBlocking {
        repository.search(
            ProfileSearchFilters(country = " India ", state = "  ", city = "", minAge = 25, maxAge = 32),
            page = 2
        )

        val call = api.lastSearch!!
        assertEquals("India", call.country)
        assertNull(call.state)
        assertNull(call.city)
        assertEquals(25, call.minAge)
        assertEquals(32, call.maxAge)
        assertEquals(2, call.page)
        assertEquals(SEARCH_PAGE_SIZE, call.pageSize)
    }

    @Test
    fun unspecifiedFiltersAreAllNull() = runBlocking {
        repository.search(ProfileSearchFilters(), page = 0)

        val call = api.lastSearch!!
        assertNull(call.country); assertNull(call.state); assertNull(call.city)
        assertNull(call.minAge); assertNull(call.maxAge)
    }

    @Test
    fun pageAndPageSizeAreBoundedBeforeTheRequest() = runBlocking {
        repository.search(ProfileSearchFilters(), page = -5, pageSize = 1000)
        assertEquals(0, api.lastSearch!!.page)
        assertEquals(MAX_SEARCH_PAGE_SIZE, api.lastSearch!!.pageSize)

        repository.search(ProfileSearchFilters(), page = 1, pageSize = 0)
        assertEquals(1, api.lastSearch!!.pageSize)
    }

    @Test
    fun unauthorizedRequiresAuthentication() = runBlocking {
        api.searchResult = { unauthorized() }

        val result = repository.search(ProfileSearchFilters(), page = 0)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals("UNAUTHORIZED", error.code)
        assertTrue(error.requiresAuthentication)
        assertTrue(error.isAuthenticationFailure())
    }

    @Test
    fun serverErrorUsesTheServerMessage() = runBlocking {
        api.searchResult = {
            Response.error(
                400,
                """{"code":"SEARCH_VALIDATION_ERROR","message":"Minimum age must be between 18 and 100."}"""
                    .toResponseBody()
            )
        }

        val error = (repository.search(ProfileSearchFilters(), page = 0) as AppResult.Failure).error

        assertEquals("SEARCH_VALIDATION_ERROR", error.code)
        assertEquals("Minimum age must be between 18 and 100.", error.message)
        assertFalse(error.requiresAuthentication)
    }

    @Test
    fun serverErrorWithoutBodyUsesAGenericMessage() = runBlocking {
        api.searchResult = { Response.error(500, "".toResponseBody()) }

        val error = (repository.search(ProfileSearchFilters(), page = 0) as AppResult.Failure).error

        assertEquals("The request could not be completed.", error.message)
    }

    @Test
    fun networkFailureMapsToASafeMessage() = runBlocking {
        api.searchResult = { throw IOException("connection details must not leak") }

        val error = (repository.search(ProfileSearchFilters(), page = 0) as AppResult.Failure).error

        assertEquals("NETWORK_ERROR", error.code)
        assertFalse(error.message.contains("connection details"))
    }

    @Test
    fun emptyBodyIsAFailure() = runBlocking {
        api.searchResult = { Response.success<ProfileSearchResponseDto>(null) }

        val error = (repository.search(ProfileSearchFilters(), page = 0) as AppResult.Failure).error

        assertEquals("EMPTY_RESPONSE", error.code)
    }

    @Test
    fun malformedBodyIsMappedNotThrown() = runBlocking {
        // Gson leaves missing fields null even for non-null Kotlin types.
        api.searchResult = { Response.success(Gson().fromJson("{}", ProfileSearchResponseDto::class.java)) }

        val result = repository.search(ProfileSearchFilters(), page = 0)

        assertTrue(result is AppResult.Failure)
        assertEquals("CLIENT_ERROR", (result as AppResult.Failure).error.code)
    }

    @Test
    fun cancellationIsNotSwallowed() = runBlocking {
        api.searchResult = { throw CancellationException("cancelled") }

        val thrown = try {
            repository.search(ProfileSearchFilters(), page = 0)
            null
        } catch (e: CancellationException) {
            e
        }

        assertNotNull(thrown)
    }

    @Test
    fun getProfileMapsDiscoverySafeFields() = runBlocking {
        val result = repository.getProfile("profile-1")

        assertEquals("profile-1", api.lastProfileId)
        val profile = (result as AppResult.Success).value
        assertEquals("Asha", profile.displayName)
        assertEquals(29, profile.age)
    }

    @Test
    fun getProfileNotFoundIsAnErrorWithTheServerMessage() = runBlocking {
        api.profileResult = {
            Response.error(404, """{"code":"PROFILE_NOT_FOUND","message":"Profile is not available."}""".toResponseBody())
        }

        val error = (repository.getProfile("gone") as AppResult.Failure).error

        assertEquals("PROFILE_NOT_FOUND", error.code)
        assertFalse(error.requiresAuthentication)
    }

    @Test
    fun getProfileUnauthorizedRequiresAuthentication() = runBlocking {
        api.profileResult = { unauthorized() }

        assertTrue((repository.getProfile("p") as AppResult.Failure).error.requiresAuthentication)
    }

    @Test
    fun getPhotoBytesReturnsBytesAndRejectsEmptyOrUnauthorized() = runBlocking {
        assertEquals(5, ((repository.getPhotoBytes("p") as AppResult.Success).value).size)

        api.photoResult = { Response.success("".toResponseBody(null)) }
        assertEquals("EMPTY_RESPONSE", (repository.getPhotoBytes("p") as AppResult.Failure).error.code)

        api.photoResult = { unauthorized() }
        assertTrue((repository.getPhotoBytes("p") as AppResult.Failure).error.requiresAuthentication)
    }

    companion object {
        private fun <T> unauthorized(): Response<T> = Response.error(
            401, """{"code":"UNAUTHORIZED","message":"Authentication required."}""".toResponseBody()
        )

        private fun item(id: String, name: String) = ProfileSearchItemDto(
            profileId = id, displayName = name, age = 29, country = "India", stateProvince = "Telangana",
            city = "Hyderabad", profession = "Engineer", skills = "Kotlin", salaryRange = "10-15 LPA",
            linkedinUrl = "https://www.linkedin.com/in/x", instagramUrl = null, facebookUrl = null,
            websiteUrl = null, verified = true, photoAvailable = false, photoUrl = null
        )

        private fun page(items: List<ProfileSearchItemDto>, total: Long = items.size.toLong(), pages: Int = 1) =
            ProfileSearchResponseDto(items, 0, 20, total, pages)

        private fun detail() = ProfileDetailDto(
            profileId = "profile-1", displayName = "Asha", age = 29, country = "India", stateProvince = "Telangana",
            city = "Hyderabad", profession = null, skills = null, salaryRange = null, linkedinUrl = null,
            instagramUrl = null, facebookUrl = null, websiteUrl = null, verified = false,
            photoAvailable = false, photoUrl = null
        )
    }
}
