package com.matrimonyapp.ui.search

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.matrimonyapp.data.discovery.SearchFilterErrors
import com.matrimonyapp.data.discovery.SearchState

/** In-memory only. Photos of other members are never written to disk. */
class PhotoCache(maxBytes: Int = 16 * 1024 * 1024) {
    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(profileId: String): Bitmap? = cache.get(profileId)

    fun put(profileId: String, bitmap: Bitmap) {
        cache.put(profileId, bitmap)
    }
}

/**
 * Screen state hoisted above the Search tab so filters, results and scroll-independent
 * state survive opening a profile and coming back. It holds no networking logic;
 * all requests go through ProfileSearchRepository.
 */
class SearchScreenModel {
    var country by mutableStateOf("")
    var state by mutableStateOf("")
    var city by mutableStateOf("")
    var minAge by mutableStateOf("")
    var maxAge by mutableStateOf("")
    var fieldErrors by mutableStateOf(SearchFilterErrors())
    var search by mutableStateOf(SearchState())
    var selectedProfileId by mutableStateOf<String?>(null)
    val photoCache = PhotoCache()

    fun clearFilters() {
        country = ""
        state = ""
        city = ""
        minAge = ""
        maxAge = ""
        fieldErrors = SearchFilterErrors()
        search = search.reset()
    }
}
