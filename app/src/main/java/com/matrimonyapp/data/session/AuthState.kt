package com.matrimonyapp.data.session

import com.matrimonyapp.data.remote.AuthenticatedUser

sealed interface AuthState {
    data object Unknown : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthenticatedUser) : AuthState
}
