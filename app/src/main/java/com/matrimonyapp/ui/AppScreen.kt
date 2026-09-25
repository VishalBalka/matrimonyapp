package com.matrimonyapp.ui

sealed interface AppScreen {
    data object Startup : AppScreen
    data object Welcome : AppScreen
    data object Login : AppScreen
    data object Registration : AppScreen
}
