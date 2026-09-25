package com.matrimonyapp.data.remote

import com.matrimonyapp.BuildConfig
import com.matrimonyapp.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    internal val BASE_URL: String = BuildConfig.API_BASE_URL.ensureApiBaseUrl()

    /**
     * The single place where the Bearer token is attached and where a 401 on an
     * authenticated request clears the stored token. Shared by every API interface.
     */
    internal fun authInterceptor(tokenStore: TokenStore): Interceptor = Interceptor { chain ->
        val token = tokenStore.get()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)

        if (response.code == 401 && request.header("Authorization") != null) {
            tokenStore.clear()
        }

        response
    }

    private val mockBackendInterceptor = MockBackendInterceptor()

    private fun retrofit(tokenStore: TokenStore): Retrofit {
        val interceptor = authInterceptor(tokenStore)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(mockBackendInterceptor)
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun create(tokenStore: TokenStore): AuthApi =
        retrofit(tokenStore).create(AuthApi::class.java)

    fun createProfileApi(tokenStore: TokenStore): ProfileApi =
        retrofit(tokenStore).create(ProfileApi::class.java)

    fun createProfileSearchApi(tokenStore: TokenStore): ProfileSearchApi =
        retrofit(tokenStore).create(ProfileSearchApi::class.java)

    fun createSecurityApi(tokenStore: TokenStore): SecurityApi =
        retrofit(tokenStore).create(SecurityApi::class.java)

    fun createNotificationApi(tokenStore: TokenStore): NotificationApi =
        retrofit(tokenStore).create(NotificationApi::class.java)

    fun createAdminApi(tokenStore: TokenStore): AdminApi =
        AdminApi(retrofit(tokenStore).create(AdminService::class.java))

    /**
     * Server-sent events use the same authentication interceptor as every other call. Only the read
     * timeout differs: the stream stays open and the server sends nothing between events.
     */
    fun createEventSourceFactory(tokenStore: TokenStore): EventSource.Factory {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor(tokenStore))
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        return EventSources.createFactory(client)
    }
}


private fun String.ensureApiBaseUrl(): String {
    val normalized = trim().let { if (it.endsWith("/")) it else "$it/" }
    if (BuildConfig.DEBUG) return normalized
    require(normalized.startsWith("https://", ignoreCase = true)) {
        "Release builds require an HTTPS API base URL."
    }
    return normalized
}
