package com.example.aquaforecast.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.AppButton
import com.example.aquaforecast.ui.components.AppCard
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.AppOutlinedTextField
import com.example.aquaforecast.ui.components.SectionHeader
import org.koin.androidx.compose.koinViewModel

private const val TAG = "SettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPondManagement: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Settings saved successfully",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Authentication Section
                SectionHeader(title = "Authentication")

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.isAuthenticated) {
                            // Signed in state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Signed In",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    state.userEmail?.let { email ->
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Authenticated",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            AppButton(
                                text = "Sign Out",
                                onClick = viewModel::signOut,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Not signed in state
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Not Signed In",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Sign in to sync your data across devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AppButton(
                                    text = "Sign In",
                                    onClick = onNavigateToLogin,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Sync Options Section
                SectionHeader(title = "Sync Options")

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column {
                        // Sync Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync Data",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (!state.isAuthenticated) {
                                    Text(
                                        text = "Sign in required",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            AppIconButton(
                                icon = Icons.Default.Sync,
                                onClick = viewModel::syncData,
                                enabled = !state.isSyncing && state.isAuthenticated && !state.isOfflineMode,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider()

                        // Offline Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Mode",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Disable all network requests",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = state.isOfflineMode,
                                onCheckedChange = { viewModel.toggleOfflineMode() }
                            )
                        }
                    }
                }

                // Forecast Options Section
                SectionHeader(title = "Forecast Options")
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppOutlinedTextField(
                            value = state.forecastHorizon,
                            onValueChange = viewModel::onForecastHorizonChanged,
                            placeholder = "Enter forecast horizon in days",
                            label = "Forecast Horizon (days)",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            enabled = !state.isSaving,
                            isError = state.forecastHorizonError != null,
                            errorMessage = state.forecastHorizonError
                        )
                    }
                }

                // Save Forecast Settings Button
                AppButton(
                    text = if (state.isSaving) "Saving..." else "Save Forecast Settings",
                    onClick = viewModel::saveForecastSettings,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                // Pond Management Section
                SectionHeader(title = "Pond Management")

                AppButton(
                    text = "Manage Ponds",
                    onClick = onNavigateToPondManagement,
                    modifier = Modifier.fillMaxWidth()
                )



                // Error message
                if (state.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.error.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}