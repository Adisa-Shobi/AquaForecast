package com.example.aquaforecast.ui.dataentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.AppButton
import com.example.aquaforecast.ui.components.AppCard
import com.example.aquaforecast.ui.components.SectionHeader
import com.example.aquaforecast.ui.components.WaterQualityTextField
import org.koin.androidx.compose.koinViewModel

private const val TAG = "DataEntryScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEntryScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: EntryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    // Show error message
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.pondId == null) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refreshPondInfo,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Pond Configured",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val annotatedText = buildAnnotatedString {
                        append("Please go to the ")
                        withLink(
                            LinkAnnotation.Clickable(
                                "tag",
                                TextLinkStyles(
                                    style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = FontStyle.Italic,
                                )),
                                {
                                    onNavigateToSettings()
                                }
                            )
                        ) {
                            append("Settings screen")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {

                        }
                        append(" to configure your pond, then pull down to refresh.")
                    }

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Water Quality Data Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Content with Pull-to-Refresh
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refreshPondInfo,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Understated pond info
                        state.pondName?.let { pondName ->
                            Text(
                                text = "Pond: $pondName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Water Parameters Section
                        SectionHeader(title = "Water Parameters")

                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Temperature
                                WaterQualityTextField(
                                    label = "Temperature (°C)",
                                    value = state.temperature,
                                    onValueChange = viewModel::onTemperatureChange,
                                    placeholder = "Enter temperature",
                                    status = state.temperatureStatus,
                                    enabled = !state.isSaving
                                )

                                // pH
                                WaterQualityTextField(
                                    label = "pH",
                                    value = state.ph,
                                    onValueChange = viewModel::onPhChange,
                                    placeholder = "Enter pH value",
                                    status = state.phStatus,
                                    enabled = !state.isSaving
                                )

                                // Dissolved Oxygen
                                WaterQualityTextField(
                                    label = "Dissolved Oxygen (mg/L)",
                                    value = state.dissolvedOxygen,
                                    onValueChange = viewModel::onDissolvedOxygenChange,
                                    placeholder = "Enter value",
                                    status = state.dissolvedOxygenStatus,
                                    enabled = !state.isSaving
                                )

                                // Ammonia
                                WaterQualityTextField(
                                    label = "Ammonia (mg/L)",
                                    value = state.ammonia,
                                    onValueChange = viewModel::onAmmoniaChange,
                                    placeholder = "Enter value",
                                    status = state.ammoniaStatus,
                                    enabled = !state.isSaving
                                )

                                // Nitrate
                                WaterQualityTextField(
                                    label = "Nitrate (mg/L)",
                                    value = state.nitrate,
                                    onValueChange = viewModel::onNitrateChange,
                                    placeholder = "Enter value",
                                    status = state.nitrateStatus,
                                    enabled = !state.isSaving
                                )

                                // Turbidity
                                WaterQualityTextField(
                                    label = "Turbidity (NTU)",
                                    value = state.turbidity,
                                    onValueChange = viewModel::onTurbidityChange,
                                    placeholder = "Enter value",
                                    status = state.turbidityStatus,
                                    enabled = !state.isSaving
                                )
                            }
                        }

                        // Save Button
                        AppButton(
                            text = "Save Offline",
                            onClick = viewModel::saveData,
                            enabled = state.canSave && !state.isSaving,
                            isLoading = state.isSaving,
                            icon = Icons.Default.Save,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Info text
                        Text(
                            text = "Data will be saved locally and sync when online.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        // Spacer for bottom navigation
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
