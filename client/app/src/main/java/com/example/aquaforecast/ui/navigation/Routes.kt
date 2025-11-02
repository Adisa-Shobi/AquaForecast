package com.example.aquaforecast.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Main : Screen("main?tab={tab}") {
        fun createRoute(tab: String? = null) = if (tab != null) "main?tab=$tab" else "main"
    }
    data object CreateSchedule : Screen("create_schedule")
    data object EditSchedule : Screen("edit_schedule/{scheduleId}") {
        fun createRoute(scheduleId: Long) = "edit_schedule/$scheduleId"
    }
    data object PondManagement : Screen("pond_management")
    data object CreatePond : Screen("create_pond")
    data object EditPond : Screen("edit_pond/{pondId}") {
        fun createRoute(pondId: Long) = "edit_pond/$pondId"
    }
}