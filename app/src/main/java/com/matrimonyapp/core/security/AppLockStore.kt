package com.matrimonyapp.core.security

import android.content.Context

class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean) { prefs.edit().putBoolean("enabled", enabled).apply() }
}