package com.matrimonyapp.ui.search

/**
 * Other members' photos are untrusted input: the server only verifies the file
 * signature, so a tiny file can declare enormous dimensions. Decoding is therefore
 * bounded: read the header first, refuse absurd sizes, and always down-sample.
 */
const val PHOTO_TARGET_PX = 512
const val MAX_SOURCE_PIXELS = 40_000_000L

/** True when the declared dimensions are positive and within the accepted pixel budget. */
fun isDecodableSize(width: Int, height: Int): Boolean =
    width > 0 && height > 0 && width.toLong() * height.toLong() <= MAX_SOURCE_PIXELS

/** Largest power-of-two sample size that keeps both dimensions at or above targetPx. */
fun calculateInSampleSize(width: Int, height: Int, targetPx: Int): Int {
    if (width <= 0 || height <= 0 || targetPx <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= targetPx && height / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample
}
