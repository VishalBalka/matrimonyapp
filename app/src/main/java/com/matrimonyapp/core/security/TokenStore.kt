package com.matrimonyapp.core.security

interface TokenStore {
    fun save(token: String)
    fun get(): String?
    fun clear()
}
