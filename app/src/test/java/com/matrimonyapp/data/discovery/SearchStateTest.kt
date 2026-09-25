package com.matrimonyapp.data.discovery

import com.matrimonyapp.core.result.AppError
import com.matrimonyapp.core.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchStateTest {
    private fun profile(id: String) = DiscoveryProfile(
        profileId = id, displayName = "Name $id", age = 29, country = "India", stateProvince = "Telangana",
        city = "Hyderabad", profession = null, skills = null, salaryRange = null, linkedinUrl = null,
        instagramUrl = null, facebookUrl = null, websiteUrl = null, verified = false,
        photoAvailable = false, photoUrl = null
    )

    private fun page(ids: List<String>, page: Int = 0, totalPages: Int = 1, total: Long = ids.size.toLong()) =
        AppResult.Success(ProfileSearchPage(ids.map(::profile), page, 20, total, totalPages))

    private val networkFailure = AppResult.Failure(AppError("NETWORK_ERROR", "Unable to reach the server."))
    private val filters = ProfileSearchFilters(country = "India", minAge = 25)

    private fun started(): SearchState = SearchState().beginSearch(filters)

    @Test
    fun startsInInitialPhaseWithNothingToLoad() {
        val state = SearchState()

        assertEquals(SearchPhase.INITIAL, state.phase)
        assertTrue(state.items.isEmpty())
        assertFalse(state.canLoadMore)
    }

    @Test
    fun beginSearchEntersLoadingAndClearsPreviousResults() {
        val loaded = started().let { it.applyFirstPage(it.requestId, page(listOf("a"))) }

        val loading = loaded.beginSearch(ProfileSearchFilters(city = "Kochi"))

        assertEquals(SearchPhase.LOADING, loading.phase)
        assertTrue(loading.items.isEmpty())
        assertEquals("Kochi", loading.filters.city)
        assertEquals(loaded.requestId + 1, loading.requestId)
    }

    @Test
    fun successWithItemsShowsResults() {
        val s = started()

        val result = s.applyFirstPage(s.requestId, page(listOf("a", "b"), totalPages = 2, total = 30))

        assertEquals(SearchPhase.RESULTS, result.phase)
        assertEquals(listOf("a", "b"), result.items.map { it.profileId })
        assertEquals(30L, result.totalItems)
        assertTrue(result.hasMore)
    }

    @Test
    fun successWithoutItemsIsEmptyNotError() {
        val s = started()

        val result = s.applyFirstPage(s.requestId, page(emptyList(), totalPages = 0))

        assertEquals(SearchPhase.EMPTY, result.phase)
        assertNull(result.errorMessage)
        assertFalse(result.hasMore)
    }

    @Test
    fun failureShowsErrorAndRetryRestartsWithTheSameFilters() {
        val s = started()

        val failed = s.applyFirstPage(s.requestId, networkFailure)

        assertEquals(SearchPhase.ERROR, failed.phase)
        assertEquals("Unable to reach the server.", failed.errorMessage)

        val retrying = failed.beginSearch(failed.filters)
        assertEquals(SearchPhase.LOADING, retrying.phase)
        assertEquals(filters, retrying.filters)
        assertNull(retrying.errorMessage)
    }

    @Test
    fun duplicateProfilesWithinAPageAreShownOnce() {
        val s = started()

        val result = s.applyFirstPage(s.requestId, page(listOf("a", "a", "b")))

        assertEquals(listOf("a", "b"), result.items.map { it.profileId })
    }

    @Test
    fun staleFirstPageResponseIsIgnored() {
        val first = started()
        val second = first.beginSearch(ProfileSearchFilters(city = "Kochi"))

        val afterStale = second.applyFirstPage(first.requestId, page(listOf("old")))

        assertSame(second, afterStale)
        assertEquals(SearchPhase.LOADING, afterStale.phase)
    }

    @Test
    fun loadMoreAppendsNextPageWithoutDuplicates() {
        val s = started()
        val firstPage = s.applyFirstPage(s.requestId, page(listOf("a", "b"), totalPages = 2, total = 4))
        assertEquals(1, firstPage.nextPage)

        val loading = firstPage.beginLoadMore()
        assertTrue(loading.loadingMore)
        // "b" reappears because a profile was inserted between the two requests.
        val merged = loading.applyNextPage(loading.requestId, page(listOf("b", "c"), page = 1, totalPages = 2, total = 4))

        assertEquals(listOf("a", "b", "c"), merged.items.map { it.profileId })
        assertEquals(1, merged.page)
        assertFalse(merged.loadingMore)
        assertFalse(merged.hasMore)
        assertFalse(merged.canLoadMore)
    }

    @Test
    fun cannotLoadMoreBeforeResultsOrOnTheLastPage() {
        val initial = SearchState()
        assertSame(initial, initial.beginLoadMore())

        val s = started()
        val single = s.applyFirstPage(s.requestId, page(listOf("a"), totalPages = 1))
        assertFalse(single.canLoadMore)
        assertSame(single, single.beginLoadMore())
    }

    @Test
    fun doubleTapOnLoadMoreDoesNotStartASecondRequest() {
        val s = started()
        val loaded = s.applyFirstPage(s.requestId, page(listOf("a"), totalPages = 3))

        val loading = loaded.beginLoadMore()

        assertFalse(loading.canLoadMore)
        assertSame(loading, loading.beginLoadMore())
    }

    @Test
    fun loadMoreFailureKeepsExistingResultsAndAllowsRetry() {
        val s = started()
        val loaded = s.applyFirstPage(s.requestId, page(listOf("a", "b"), totalPages = 2))
        val loading = loaded.beginLoadMore()

        val failed = loading.applyNextPage(loading.requestId, networkFailure)

        assertEquals(SearchPhase.RESULTS, failed.phase)
        assertEquals(listOf("a", "b"), failed.items.map { it.profileId })
        assertEquals("Unable to reach the server.", failed.loadMoreError)
        assertFalse(failed.loadingMore)
        assertEquals(1, failed.nextPage)
        assertTrue(failed.canLoadMore)

        val retry = failed.beginLoadMore()
        assertTrue(retry.loadingMore)
        assertNull(retry.loadMoreError)
    }

    @Test
    fun loadMoreResponseAfterANewSearchIsIgnored() {
        val s = started()
        val loaded = s.applyFirstPage(s.requestId, page(listOf("a"), totalPages = 2))
        val loading = loaded.beginLoadMore()
        val newSearch = loading.beginSearch(ProfileSearchFilters(city = "Kochi"))

        val result = newSearch.applyNextPage(loading.requestId, page(listOf("late"), page = 1, totalPages = 2))

        assertSame(newSearch, result)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun resetReturnsToInitialAndInvalidatesInFlightResponses() {
        val s = started()

        val cleared = s.reset()
        val afterLateResponse = cleared.applyFirstPage(s.requestId, page(listOf("late")))

        assertEquals(SearchPhase.INITIAL, cleared.phase)
        assertTrue(cleared.items.isEmpty())
        assertSame(cleared, afterLateResponse)
    }

    @Test
    fun authenticationFailuresAreRecognisedForTheExistingExpirationFlow() {
        assertTrue(AppError("UNAUTHORIZED", "x", requiresAuthentication = true).isAuthenticationFailure())
        assertTrue(AppError("UNAUTHORIZED", "x").isAuthenticationFailure())
        assertTrue(AppError("AUTHENTICATION_REQUIRED", "x").isAuthenticationFailure())
        assertFalse(AppError("NETWORK_ERROR", "x").isAuthenticationFailure())
        assertFalse(AppError("PROFILE_NOT_FOUND", "x").isAuthenticationFailure())
    }
}
