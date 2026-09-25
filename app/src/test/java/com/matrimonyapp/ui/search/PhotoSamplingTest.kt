package com.matrimonyapp.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSamplingTest {
    @Test
    fun smallImagesAreNotSubsampled() {
        assertEquals(1, calculateInSampleSize(400, 400, 512))
        assertEquals(1, calculateInSampleSize(512, 512, 512))
    }

    @Test
    fun largeImagesAreSubsampledByPowersOfTwoAndStayAtOrAboveTarget() {
        assertEquals(4, calculateInSampleSize(4000, 3000, 512))
        assertEquals(2, calculateInSampleSize(1024, 1024, 512))
        val sample = calculateInSampleSize(8000, 6000, 512)
        assertTrue(6000 / sample >= 512)
        assertEquals(0, sample and (sample - 1)) // power of two
    }

    @Test
    fun invalidDimensionsFallBackToNoSubsampling() {
        assertEquals(1, calculateInSampleSize(0, 100, 512))
        assertEquals(1, calculateInSampleSize(100, -1, 512))
        assertEquals(1, calculateInSampleSize(100, 100, 0))
    }

    @Test
    fun ordinaryPhotosAreDecodable() {
        assertTrue(isDecodableSize(4000, 3000))
        assertTrue(isDecodableSize(6000, 6000))
    }

    @Test
    fun decompressionBombDimensionsAreRefused() {
        assertFalse(isDecodableSize(30_000, 30_000))
        assertFalse(isDecodableSize(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun nonPositiveDimensionsAreRefused() {
        assertFalse(isDecodableSize(0, 100))
        assertFalse(isDecodableSize(100, 0))
        assertFalse(isDecodableSize(-1, -1))
    }
}
