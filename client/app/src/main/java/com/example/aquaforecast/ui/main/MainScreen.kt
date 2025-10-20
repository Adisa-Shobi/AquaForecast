package com.example.aquaforecast.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aquaforecast.ui.dashboard.DashboardScreen
import com.example.aquaforecast.ui.dataentry.DataEntryScreen
import com.example.aquaforecast.ui.predictions.PredictionsScreen
import com.example.aquaforecast.ui.schedule.ScheduleScreen
import com.example.aquaforecast.ui.settings.SettingsScreen

private const val TAG = "MainScreen"

/**
 * Main screen with bottom navigation
 */
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(DashboardTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                    DashboardTab.entries.forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.iconSelected else tab.iconUnselected,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                            }
                        )
                    }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                DashboardTab.DASHBOARD -> DashboardScreen(
                    {selectedTab = DashboardTab.DATA_ENTRY},
                    {selectedTab = DashboardTab.SETTINGS},
                    {selectedTab = DashboardTab.PREDICTIONS},
                    {selectedTab = DashboardTab.SCHEDULE}
                )
                DashboardTab.DATA_ENTRY  -> DataEntryScreen()
                DashboardTab.PREDICTIONS  -> PredictionsScreen()
                DashboardTab.SCHEDULE -> ScheduleScreen()
                DashboardTab.SETTINGS-> SettingsScreen()
            }
        }
    }
}

enum class DashboardTab(val title: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    DATA_ENTRY("Data Entry", Icons.Filled.Add, Icons.Outlined.Add),
    PREDICTIONS("Predictions", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    SCHEDULE("Schedule", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}