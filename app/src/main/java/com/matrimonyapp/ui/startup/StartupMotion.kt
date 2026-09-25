package com.matrimonyapp.ui.startup

import kotlin.math.roundToLong

private const val BASE_SEQUENCE_MS = 1900L

/**
 * Converts the system animator-duration scale into a usable animation duration.
 *
 * A scale of 0 means reduced motion is requested and the animation should be skipped.
 * Positive values preserve the system timing preference.
 */
fun startupDurationMillis(
    animatorDurationScale: Float,
    baseDurationMillis: Long = BASE_SEQUENCE_MS
): Long {
    if (animatorDurationScale <= 0f) return 0L
    return (baseDurationMillis * animatorDurationScale).roundToLong()
}
