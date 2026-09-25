package com.matrimonyapp.ui.search

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** See PhotoSampling.kt for why decoding is bounded. */
fun decodeSampledBitmap(bytes: ByteArray, targetPx: Int = PHOTO_TARGET_PX): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (!isDecodableSize(bounds.outWidth, bounds.outHeight)) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: RuntimeException) {
        null
    }
}
