package com.matrimonyapp.data.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {
    @Test
    fun validProfilePasses() {
        assertTrue(validateProfile("Alex", "1995-05-20", "MALE", "Hyderabad", "Hello").isValid)
    }

    @Test
    fun invalidGenderFails() {
        assertFalse(validateProfile("Alex", "1995-05-20", "INVALID", "Hyderabad", "").isValid)
    }

    @Test
    fun invalidDateFormatFails() {
        assertFalse(validateProfile("Alex", "20-05-1995", "MALE", "Hyderabad", "").isValid)
    }

    @Test
    fun longBioFails() {
        assertFalse(validateProfile("Alex", "1995-05-20", "MALE", "Hyderabad", "x".repeat(501)).isValid)
    }

    @Test
    fun acceptsEnrichedProfile() {
        assertTrue(
            validateProfile(
                "Alex", "1995-05-20", "MALE", "India", "Telangana",
                "Hyderabad", "Looking for a meaningful connection.", "+91 98765 43210"
            ).isValid
        )
    }

    @Test
    fun rejectsMissingLocation() {
        assertFalse(
            validateProfile("Alex", "1995-05-20", "MALE", "", "", "Hyderabad", "", "").isValid
        )
    }

    @Test
    fun rejectsInvalidPhone() {
        assertFalse(
            validateProfile("Alex", "1995-05-20", "MALE", "India", "Telangana", "Hyderabad", "", "abc123").isValid
        )
    }
}
