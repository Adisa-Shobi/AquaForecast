package com.example.aquaforecast.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Prediction : Screen("prediction")
    data object PondSetup : Screen("pond_setup")
    data object Settings : Screen("settings")
}