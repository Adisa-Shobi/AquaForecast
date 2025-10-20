package com.example.aquaforecast.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.EmptyState
import com.example.aquaforecast.ui.components.ErrorState
import com.example.aquaforecast.ui.components.LoadingState
import com.example.aquaforecast.ui.dashboard.components.*
import org.koin.androidx.compose.koinViewModel

private const val TAG = "DashboardScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDataEntry: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPredictions: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToDataEntry,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry"
                )
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading && !state.isRefreshing -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading dashboard..."
                )
            }

            state.showEmptyState -> {
                EmptyState(
                    title = "No Data Yet",
                    message = "Start by adding your first farm data entry to see predictions and insights",
                    actionText = "Add First Entry",
                    onActionClick = onNavigateToDataEntry,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            state.error != null && !state.hasData -> {
                ErrorState(
                    message = state.error ?: "Unknown error",
                    onActionClick = { viewModel.loadDashboardData() },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                DashboardContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline mode banner
            if (state.isOfflineMode) {
                item {
                    OfflineModeBanner()
                }
            }

            // Yield prediction card
            item {
                YieldPredictionCard(
                    prediction = state.latestPrediction,
                    stockCount = state.pond?.stockCount ?: 0
                )
            }

            // Pond status section
            if (state.pond != null) {
                item {
                    val pondStatus = createPondStatus(
                        pond = state.pond,
                        farmData = state.latestFarmData
                    )
                    PondStatusSection(
                        ponds = listOf(pondStatus)
                    )
                }
            }

            // Feeding reminders section
            item {
                val feedingReminders = listOf(
                    FeedingReminder(
                        pondName = state.pond?.name ?: "Pond A",
                        time = "8:00 AM"
                    ),
                    FeedingReminder(
                        pondName = state.pond?.name ?: "Pond B",
                        time = "10:00 AM"
                    )
                )
                FeedingRemindersSection(feedingReminders = feedingReminders)
            }
        }
    }
}

