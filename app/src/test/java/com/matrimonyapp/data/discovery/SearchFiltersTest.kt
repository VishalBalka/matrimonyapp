package com.matrimonyapp.data.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFiltersTest {
    private fun validate(
        country: String = "", state: String = "", city: String = "", min: String = "", max: String = ""
    ) = validateSearchFilters(country, state, city, min, max)

    private fun valid(v: SearchFilterValidation) = (v as SearchFilterValidation.Valid).filters
    private fun errors(v: SearchFilterValidation) = (v as SearchFilterValidation.Invalid).errors

    @Test
    fun emptyFormIsValidAndRestrictsNothing() {
        val filters = valid(validate())

        assertNull(filters.country); assertNull(filters.state); assertNull(filters.city)
        assertNull(filters.minAge); assertNull(filters.maxAge)
    }

    @Test
    fun valuesAreTrimmedAndParsed() {
        val filters = valid(validate("  India ", " Telangana", "Hyderabad  ", " 25 ", "32"))

        assertEquals("India", filters.country)
        assertEquals("Telangana", filters.state)
        assertEquals("Hyderabad", filters.city)
        assertEquals(25, filters.minAge)
        assertEquals(32, filters.maxAge)
    }

    @Test
    fun whitespaceOnlyTextIsTreatedAsNotSupplied() {
        assertNull(valid(validate(country = "   ")).country)
    }

    @Test
    fun ageBoundsAreInclusive() {
        val filters = valid(validate(min = "18", max = "100"))
        assertEquals(18, filters.minAge)
        assertEquals(100, filters.maxAge)
        assertTrue(validate(min = "30", max = "30") is SearchFilterValidation.Valid)
    }

    @Test
    fun agesOutsideSupportedRangeAreRejected() {
        assertNotNull(errors(validate(min = "17")).minAge)
        assertNotNull(errors(validate(min = "0")).minAge)
        assertNotNull(errors(validate(max = "101")).maxAge)
        assertNotNull(errors(validate(max = "999")).maxAge)
    }

    @Test
    fun nonNumericAgesAreRejected() {
        assertNotNull(errors(validate(min = "abc")).minAge)
        assertNotNull(errors(validate(max = "-5")).maxAge)
        assertNotNull(errors(validate(min = "2.5")).minAge)
        assertNotNull(errors(validate(max = "1234")).maxAge)
    }

    @Test
    fun minimumGreaterThanMaximumIsRejected() {
        val e = errors(validate(min = "40", max = "30"))
        assertEquals("Minimum age cannot be greater than maximum age.", e.minAge)
        assertNull(e.maxAge)
    }

    @Test
    fun overlongTextIsRejected() {
        assertNotNull(errors(validate(country = "x".repeat(101))).country)
        assertNotNull(errors(validate(state = "x".repeat(101))).state)
        assertNotNull(errors(validate(city = "x".repeat(121))).city)
        assertTrue(validate(country = "x".repeat(100), city = "x".repeat(120)) is SearchFilterValidation.Valid)
    }

    @Test
    fun controlCharactersAreRejected() {
        assertNotNull(errors(validate(city = "Hyder\nabad")).city)
        assertNotNull(errors(validate(country = "In\u0000dia")).country)
    }

    @Test
    fun allErrorsAreReportedTogether() {
        val e = errors(validate(country = "x".repeat(200), min = "abc", max = "500"))

        assertTrue(e.hasErrors)
        assertNotNull(e.country); assertNotNull(e.minAge); assertNotNull(e.maxAge)
        assertNull(e.state); assertNull(e.city)
    }

    @Test
    fun noErrorsMeansHasErrorsIsFalse() {
        assertFalse(SearchFilterErrors().hasErrors)
    }
}
