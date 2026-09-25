package com.matrimonyapp.ui.startup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupMotionTest {

    @Test
    fun zeroAnimatorScaleSkipsAnimation() {
        assertEquals(0L, startupDurationMillis(0f))
    }

    @Test
    fun positiveAnimatorScaleProducesPositiveDuration() {
        assertTrue(startupDurationMillis(1f) > 0L)
    }

    @Test
    fun animatorScaleMultipliesBaseDuration() {
        assertEquals(3800L, startupDurationMillis(2f))
    }

    @Test
    fun negativeAnimatorScaleIsTreatedAsReducedMotion() {
        assertEquals(0L, startupDurationMillis(-1f))
    }
}
