package com.matrimonyapp.data.remote

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ApiErrorMapperTest {
    @Test
    fun unauthorizedRequiresAuthentication() {
        val response = Response.error<Any>(
            401,
            """{"code":"UNAUTHORIZED","message":"Authentication required."}""".toResponseBody()
        )

        val error = ApiErrorMapper.fromResponse(response)

        assertEquals("UNAUTHORIZED", error.code)
        assertTrue(error.requiresAuthentication)
    }

    @Test
    fun conflictUsesServerMessage() {
        val response = Response.error<Any>(
            409,
            """{"code":"EMAIL_ALREADY_EXISTS","message":"An account with this email already exists."}""".toResponseBody()
        )

        val error = ApiErrorMapper.fromResponse(response)

        assertEquals("EMAIL_ALREADY_EXISTS", error.code)
        assertEquals("An account with this email already exists.", error.message)
    }

    @Test
    fun networkFailureMapsToSafeMessage() {
        val error = ApiErrorMapper.fromThrowable(java.io.IOException("not exposed"))

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.message.contains("Unable to reach the server"))
    }
}
