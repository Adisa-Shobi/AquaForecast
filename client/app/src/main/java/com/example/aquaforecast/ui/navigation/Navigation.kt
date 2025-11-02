package com.example.aquaforecast.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aquaforecast.ui.auth.LoginScreen
import com.example.aquaforecast.ui.main.MainScreen
import com.example.aquaforecast.ui.pondmanagement.CreatePondScreen
import com.example.aquaforecast.ui.pondmanagement.PondFormViewModel
import com.example.aquaforecast.ui.pondmanagement.PondManagementScreen
import com.example.aquaforecast.ui.schedule.CreateScheduleScreen
import com.example.aquaforecast.ui.schedule.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Navigation() {
    val navController = rememberNavController()

    // Create shared ViewModel for Schedule-related screens
    val scheduleViewModel: ScheduleViewModel = koinViewModel()

    NavHost(navController = navController, Screen.Main.route) {
        composable(route = Screen.Login.route) {
            LoginScreen({
                navController.navigate(Screen.Main.createRoute("SETTINGS")) {
                    popUpTo(Screen.Login.route) {
                        inclusive=true
                    }
                }
            })
        }
        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab")
            MainScreen(
                initialTab = initialTab,
                scheduleViewModel = scheduleViewModel,
                onNavigateToCreateSchedule = {
                    navController.navigate(Screen.CreateSchedule.route)
                },
                onNavigateToEditSchedule = { scheduleId ->
                    navController.navigate(Screen.EditSchedule.createRoute(scheduleId))
                },
                onNavigateToPondManagement = {
                    navController.navigate(Screen.PondManagement.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(route = Screen.CreateSchedule.route) {
            CreateScheduleScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Main.createRoute("SCHEDULE")) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                viewModel = scheduleViewModel
            )
        }

        composable(
            route = Screen.EditSchedule.route,
            arguments = listOf(
                navArgument("scheduleId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getLong("scheduleId") ?: 0L
            CreateScheduleScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Main.createRoute("SCHEDULE")) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                viewModel = scheduleViewModel
            )
        }

        composable(route = Screen.PondManagement.route) {
            PondManagementScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Main.createRoute("SETTINGS")) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToCreatePond = {
                    navController.navigate(Screen.CreatePond.route)
                },
                onNavigateToEditPond = { pondId ->
                    navController.navigate(Screen.EditPond.createRoute(pondId))
                }
            )
        }

        composable(route = Screen.CreatePond.route) {
            val pondFormViewModel: PondFormViewModel = koinViewModel()
            CreatePondScreen(
                onNavigateBack = {
                    navController.navigate(Screen.PondManagement.route) {
                        popUpTo(Screen.PondManagement.route) { inclusive = true }
                    }
                },
                viewModel = pondFormViewModel
            )
        }

        composable(
            route = Screen.EditPond.route,
            arguments = listOf(
                navArgument("pondId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val pondFormViewModel: PondFormViewModel = koinViewModel()
            val pondId = backStackEntry.arguments?.getLong("pondId") ?: 0L

            LaunchedEffect(pondId) {
                if (pondId > 0) {
                    pondFormViewModel.loadPond(pondId)
                }
            }

            CreatePondScreen(
                onNavigateBack = {
                    navController.navigate(Screen.PondManagement.route) {
                        popUpTo(Screen.PondManagement.route) { inclusive = true }
                    }
                },
                viewModel = pondFormViewModel
            )
        }
    }
}
