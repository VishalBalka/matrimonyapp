package com.matrimonyapp.data.remote

import com.matrimonyapp.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises the shared authentication interceptor without any network: a terminal
 * interceptor plays the server and returns a canned status code.
 */
class ApiClientAuthInterceptorTest {
    private class FakeTokenStore(var value: String?) : TokenStore {
        override fun save(token: String) { value = token }
        override fun get(): String? = value
        override fun clear() { value = null }
    }

    private class ServerStub(private val code: Int) : Interceptor {
        var seenAuthorization: String? = null
        override fun intercept(chain: Interceptor.Chain): Response {
            seenAuthorization = chain.request().header("Authorization")
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("stub")
                .body("".toResponseBody(null))
                .build()
        }
    }

    private fun call(store: TokenStore, server: ServerStub): Int {
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiClient.authInterceptor(store))
            .addInterceptor(server)
            .build()
        client.newCall(Request.Builder().url("http://localhost/api/v1/profiles/search").build())
            .execute().use { return it.code }
    }

    @Test
    fun unauthorizedResponseOnAuthenticatedSearchClearsTheStoredToken() {
        val store = FakeTokenStore("test-only-token")
        val server = ServerStub(401)

        assertEquals(401, call(store, server))

        assertEquals("Bearer test-only-token", server.seenAuthorization)
        assertNull(store.value)
    }

    @Test
    fun successfulResponseKeepsTheToken() {
        val store = FakeTokenStore("test-only-token")

        assertEquals(200, call(store, ServerStub(200)))

        assertEquals("test-only-token", store.value)
    }

    @Test
    fun nonAuthenticationErrorsDoNotClearTheToken() {
        for (code in listOf(400, 403, 404, 429, 500)) {
            val store = FakeTokenStore("test-only-token")
            call(store, ServerStub(code))
            assertEquals("HTTP $code must not clear the token", "test-only-token", store.value)
        }
    }

    @Test
    fun noTokenMeansNoAuthorizationHeader() {
        val store = FakeTokenStore(null)
        val server = ServerStub(401)

        call(store, server)

        assertNull(server.seenAuthorization)
        assertNull(store.value)
    }
}
