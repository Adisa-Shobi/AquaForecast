package com.example.aquaforecast.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.aquaforecast.data.preferences.PreferencesManager
import com.example.aquaforecast.ui.components.OfflineBanner
import com.example.aquaforecast.ui.dashboard.DashboardScreen
import com.example.aquaforecast.ui.dataentry.DataEntryScreen
import com.example.aquaforecast.ui.datahistory.DataHistoryScreen
import com.example.aquaforecast.ui.schedule.ScheduleScreen
import com.example.aquaforecast.ui.schedule.ScheduleViewModel
import com.example.aquaforecast.ui.settings.SettingsScreen
import org.koin.compose.koinInject

private const val TAG = "MainScreen"

/**
 * Main screen with bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    scheduleViewModel: ScheduleViewModel,
    onNavigateToCreateSchedule: () -> Unit = {},
    onNavigateToEditSchedule: (Long) -> Unit = {},
    onNavigateToPondManagement: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val startTab = remember(initialTab) {
        when (initialTab) {
            "DASHBOARD" -> DashboardTab.DASHBOARD
            "DATA" -> DashboardTab.DATA
            "DATA_ENTRY" -> DashboardTab.DATA_ENTRY
            "SCHEDULE" -> DashboardTab.SCHEDULE
            "SETTINGS" -> DashboardTab.SETTINGS
            else -> DashboardTab.DASHBOARD
        }
    }
    var selectedTab by remember(startTab) { mutableStateOf(startTab) }
    val preferencesManager: PreferencesManager = koinInject()
    val isOffline by preferencesManager.offlineMode.collectAsState(initial = false)

    Scaffold(
        bottomBar = {
            NavigationBar (
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,

            ) {
                    DashboardTab.entries.forEach { tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.iconSelected else tab.iconUnselected,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                ) },
                            selected = selectedTab == tab,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                            ),
                            onClick = {
                                selectedTab = tab
                            }
                        )
                    }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Offline mode banner
            if (isOffline) {
                OfflineBanner()
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                DashboardTab.DASHBOARD -> DashboardScreen(
                    {selectedTab = DashboardTab.DATA_ENTRY},
                    {selectedTab = DashboardTab.SETTINGS},
                    {selectedTab = DashboardTab.SCHEDULE}
                )
                DashboardTab.DATA -> DataHistoryScreen(
                    onNavigateToSettings = {
                        selectedTab = DashboardTab.SETTINGS
                    },
                    onNavigateToDataEntry = {
                        selectedTab = DashboardTab.DATA_ENTRY
                    }
                )
                DashboardTab.DATA_ENTRY  -> DataEntryScreen(
                    onNavigateToSettings = { selectedTab = DashboardTab.SETTINGS },
                )
                DashboardTab.SCHEDULE -> ScheduleScreen(
                    onNavigateToCreateSchedule = onNavigateToCreateSchedule,
                    onNavigateToEditSchedule = onNavigateToEditSchedule,
                    viewModel = scheduleViewModel
                )
                DashboardTab.SETTINGS-> SettingsScreen(
                    onNavigateToPondManagement = onNavigateToPondManagement,
                    onNavigateToLogin = onNavigateToLogin
                )
                }
            }
        }
    }
}

enum class DashboardTab(val title: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    DATA("Data", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    DATA_ENTRY("Entry", Icons.Filled.Add, Icons.Outlined.Add),
    SCHEDULE("Schedule", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}