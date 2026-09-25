package com.matrimonyapp

import android.app.Application
import com.matrimonyapp.core.security.SecureTokenStore
import com.matrimonyapp.data.discovery.ProfileSearchRepository
import com.matrimonyapp.data.remote.ApiClient
import com.matrimonyapp.data.notification.NotificationRepository
import com.matrimonyapp.data.security.SecurityRepository
import com.matrimonyapp.core.security.AppLockStore
import com.matrimonyapp.data.profile.ProfileRepository
import com.matrimonyapp.data.repository.AuthRepository

class MatrimonyApplication : Application() {
    lateinit var authRepository: AuthRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var profileSearchRepository: ProfileSearchRepository
        private set
    lateinit var securityRepository: SecurityRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set
    lateinit var appLockStore: AppLockStore
        private set
    lateinit var adminApi: com.matrimonyapp.data.remote.AdminApi
        private set

    override fun onCreate() {
        super.onCreate()
        val tokenStore = SecureTokenStore(this)
        val api = ApiClient.create(tokenStore)
        val profileApi = ApiClient.createProfileApi(tokenStore)
        authRepository = AuthRepository(api, tokenStore)
        profileRepository = ProfileRepository(profileApi)
        profileSearchRepository = ProfileSearchRepository(ApiClient.createProfileSearchApi(tokenStore))
        securityRepository = SecurityRepository(ApiClient.createSecurityApi(tokenStore))
        notificationRepository = NotificationRepository(
            ApiClient.createNotificationApi(tokenStore),
            ApiClient.createEventSourceFactory(tokenStore),
            ApiClient.BASE_URL,
            tokenStore
        )
        appLockStore = AppLockStore(this)
        adminApi = ApiClient.createAdminApi(tokenStore)
    }
}
