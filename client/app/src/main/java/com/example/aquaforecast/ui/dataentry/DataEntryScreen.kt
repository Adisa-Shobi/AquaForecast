package com.example.aquaforecast.ui.dataentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.AppButton
import com.example.aquaforecast.ui.components.AppCard
import com.example.aquaforecast.ui.components.AppCompactButton
import com.example.aquaforecast.ui.components.AppCompactOutlinedButton
import com.example.aquaforecast.ui.components.AppDialogButton
import com.example.aquaforecast.ui.components.AppErrorButton
import com.example.aquaforecast.ui.components.AppOutlinedTextField
import com.example.aquaforecast.ui.components.AppTextButton
import com.example.aquaforecast.ui.components.PondSelectorWithHeader
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

    // Refresh ponds when navigating to this screen
    LaunchedEffect(Unit) {
        viewModel.refreshPonds()
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Water Quality Data Entry",
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
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.pondId == null -> {
                    NoPondConfiguredState(
                        onNavigateToSettings = onNavigateToSettings,
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refreshPondInfo
                    )
                }

                else -> {
                    DataEntryContent(
                        state = state,
                        viewModel = viewModel,
                        scrollState = scrollState
                    )
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

    // Dialogs
    if (state.showReportDeathDialog) {
        ReportDeathDialog(
            onDismiss = viewModel::hideReportDeathDialog,
            onConfirm = { deathCount -> viewModel.reportFishDeaths(deathCount) }
        )
    }

    if (state.showHarvestDialog) {
        HarvestConfirmationDialog(
            pondName = state.selectedPond?.name ?: "",
            onDismiss = viewModel::hideHarvestDialog,
            onConfirm = viewModel::markPondAsHarvested
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoPondConfiguredState(
    onNavigateToSettings: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Pond Configured",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val annotatedText = buildAnnotatedString {
                append("Please go to the ")
                withLink(
                    LinkAnnotation.Clickable(
                "settings",
                TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic
                    )
                ),
                { onNavigateToSettings() }
                    )
                ) {
                    append("Settings screen")
                }
                append(" to configure your pond, then pull down to refresh.")
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataEntryContent(
    state: EntryState,
    viewModel: EntryViewModel,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
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
                // Harvested pond banner
                if (state.selectedPond?.isHarvested == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Harvested",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = "Pond Harvested",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "This pond has been marked as harvested. Data entry is disabled.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Pond Selection Dropdown
                if (state.availablePonds.isNotEmpty()) {
                    PondSelectorWithHeader(
                        ponds = state.availablePonds,
                        selectedPond = state.selectedPond,
                        expanded = state.isPondDropdownExpanded,
                        onExpandedChange = viewModel::togglePondDropdown,
                        onPondSelected = viewModel::onPondSelected,
                        enabled = !state.isSaving
                    )
                }

                if (state.selectedPond?.isHarvested != true) {
                    SectionHeader(title = "Pond Actions")

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Report Deaths button
                            AppCompactOutlinedButton(
                                text = "Report Deaths",
                                onClick = viewModel::showReportDeathDialog,
                                icon = Icons.Outlined.Warning,
                                modifier = Modifier.weight(1f)
                            )

                            // Mark Harvested button
                            AppCompactButton(
                                text = "Mark Harvested",
                                onClick = viewModel::showHarvestDialog,
                                icon = Icons.Outlined.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )

                        // pH
                        WaterQualityTextField(
                            label = "pH",
                            value = state.ph,
                            onValueChange = viewModel::onPhChange,
                            placeholder = "Enter pH value",
                            status = state.phStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )

                        // Dissolved Oxygen
                        WaterQualityTextField(
                            label = "Dissolved Oxygen (mg/L)",
                            value = state.dissolvedOxygen,
                            onValueChange = viewModel::onDissolvedOxygenChange,
                            placeholder = "Enter value",
                            status = state.dissolvedOxygenStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )

                        // Ammonia
                        WaterQualityTextField(
                            label = "Ammonia (mg/L)",
                            value = state.ammonia,
                            onValueChange = viewModel::onAmmoniaChange,
                            placeholder = "Enter value",
                            status = state.ammoniaStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )

                        // Nitrate
                        WaterQualityTextField(
                            label = "Nitrate (mg/L)",
                            value = state.nitrate,
                            onValueChange = viewModel::onNitrateChange,
                            placeholder = "Enter value",
                            status = state.nitrateStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )

                        // Turbidity
                        WaterQualityTextField(
                            label = "Turbidity (NTU)",
                            value = state.turbidity,
                            onValueChange = viewModel::onTurbidityChange,
                            placeholder = "Enter value",
                            status = state.turbidityStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true
                        )
                    }
                }

                // Save Button
                AppButton(
                    text = "Save Offline",
                    onClick = viewModel::saveData,
                    enabled = state.canSave && !state.isSaving && state.selectedPond?.isHarvested != true,
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

@Composable
private fun ReportDeathDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var deathCount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Report Fish Deaths",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter the number of fish that have died. This will update the stock count accordingly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppOutlinedTextField(
                    value = deathCount,
                    onValueChange = {
                        deathCount = it
                        error = null
                    },
                    label = "Number of Deaths",
                    placeholder = "Enter number",
                    keyboardType = KeyboardType.Number,
                    isError = error != null,
                    errorMessage = error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppDialogButton(
                text = "Confirm",
                onClick = {
                    val count = deathCount.toIntOrNull()
                    when {
                        count == null || count <= 0 -> {
                            error = "Please enter a valid number"
                        }
                        else -> {
                            onConfirm(count)
                        }
                    }
                }
            )
        },
        dismissButton = {
            AppDialogButton(
                text = "Cancel",
                onClick = onDismiss,
                isPrimary = false
            )
        }
    )
}

@Composable
private fun HarvestConfirmationDialog(
    pondName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Mark Pond as Harvested?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You are about to mark \"$pondName\" as harvested.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Warning:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "• This pond will become read-only\n• No new data can be added\n• Can only be reversed from Pond Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppCompactButton(
                text = "Confirm Harvest",
                onClick = onConfirm,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        },
        dismissButton = {
            AppDialogButton(
                text = "Cancel",
                onClick = onDismiss,
                isPrimary = false
            )
        }
    )
}
