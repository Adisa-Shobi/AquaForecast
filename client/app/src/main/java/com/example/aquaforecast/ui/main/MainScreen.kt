package com.example.aquaforecast.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.aquaforecast.ui.dashboard.DashboardScreen
import com.example.aquaforecast.ui.dataentry.DataEntryScreen
import com.example.aquaforecast.ui.predictions.PredictionsScreen
import com.example.aquaforecast.ui.schedule.ScheduleScreen
import com.example.aquaforecast.ui.settings.SettingsScreen

/**
 * Main screen with bottom navigation
 */
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Dashboard
                            else Icons.Outlined.Dashboard,
                            "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Add
                            else Icons.Outlined.Add,
                            "Data Entry"
                        )
                    },
                    label = { Text("Data Entry") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.TrendingUp
                            else Icons.Outlined.TrendingUp,
                            "Predictions"
                        )
                    },
                    label = { Text("Predictions") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.CalendarToday
                            else Icons.Outlined.CalendarToday,
                            "Schedule"
                        )
                    },
                    label = { Text("Schedule") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 4) Icons.Filled.Settings
                            else Icons.Outlined.Settings,
                            "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> DataEntryScreen()
                2 -> PredictionsScreen()
                3 -> ScheduleScreen()
                4 -> SettingsScreen()
            }
        }
    }
}