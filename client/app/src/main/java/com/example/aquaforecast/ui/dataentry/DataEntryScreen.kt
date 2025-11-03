package com.example.aquaforecast.ui.dataentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.res.stringResource
import com.example.aquaforecast.R
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

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.onLocationPermissionResult(granted)
    }

    // Request location permission when needed
    LaunchedEffect(state.shouldRequestLocationPermission) {
        if (state.shouldRequestLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

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
                        text = stringResource(R.string.data_entry_title),
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

    if (state.showPredictionVerificationDialog) {
        PredictionVerificationDialog(
            predictedWeight = state.predictedWeight ?: 0.0,
            predictedLength = state.predictedLength ?: 0.0,
            onDismiss = viewModel::hidePredictionVerificationDialog,
            onConfirm = { isAccurate -> viewModel.verifyPrediction(isAccurate) }
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
                SectionHeader(title = stringResource(R.string.data_entry_section_water_params))

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
                            label = stringResource(R.string.param_temperature_label),
                            value = state.temperature,
                            onValueChange = viewModel::onTemperatureChange,
                            placeholder = stringResource(R.string.param_temperature_placeholder),
                            status = state.temperatureStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_temperature_hint)
                        )

                        // pH
                        WaterQualityTextField(
                            label = stringResource(R.string.param_ph_label),
                            value = state.ph,
                            onValueChange = viewModel::onPhChange,
                            placeholder = stringResource(R.string.param_ph_placeholder),
                            status = state.phStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_ph_hint)
                        )

                        // Dissolved Oxygen
                        WaterQualityTextField(
                            label = stringResource(R.string.param_do_label),
                            value = state.dissolvedOxygen,
                            onValueChange = viewModel::onDissolvedOxygenChange,
                            placeholder = stringResource(R.string.param_do_placeholder),
                            status = state.dissolvedOxygenStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_do_hint)
                        )

                        // Ammonia
                        WaterQualityTextField(
                            label = stringResource(R.string.param_ammonia_label),
                            value = state.ammonia,
                            onValueChange = viewModel::onAmmoniaChange,
                            placeholder = stringResource(R.string.param_ammonia_placeholder),
                            status = state.ammoniaStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_ammonia_hint)
                        )

                        // Nitrate
                        WaterQualityTextField(
                            label = stringResource(R.string.param_nitrate_label),
                            value = state.nitrate,
                            onValueChange = viewModel::onNitrateChange,
                            placeholder = stringResource(R.string.param_nitrate_placeholder),
                            status = state.nitrateStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_nitrate_hint)
                        )

                        // Turbidity
                        WaterQualityTextField(
                            label = stringResource(R.string.param_turbidity_label),
                            value = state.turbidity,
                            onValueChange = viewModel::onTurbidityChange,
                            placeholder = stringResource(R.string.param_turbidity_placeholder),
                            status = state.turbidityStatus,
                            enabled = !state.isSaving && state.selectedPond?.isHarvested != true,
                            supportingText = stringResource(R.string.param_turbidity_hint)
                        )
                    }
                }

                // Location Section
                if (state.selectedPond?.isHarvested != true) {
                    LocationSection(
                        state = state,
                        onLocationTypeChange = viewModel::onLocationTypeChange,
                        onCaptureLocation = viewModel::captureLocation,
                        onRequestPermission = viewModel::requestLocationPermission
                    )
                }

                // Save Button
                AppButton(
                    text = stringResource(R.string.action_save_offline),
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

@Composable
private fun PredictionVerificationDialog(
    predictedWeight: Double,
    predictedLength: Double,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Verify Prediction",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Based on your water quality data, here are the predicted fish measurements:",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Weight prediction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Predicted Weight:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.2f kg", predictedWeight),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Length prediction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Predicted Length:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.1f cm", predictedLength),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Do these predictions look accurate?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppCompactButton(
                text = "Yes, Accurate",
                onClick = { onConfirm(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        dismissButton = {
            OutlinedButton(onClick = { onConfirm(false) }) {
                Text("No, Inaccurate")
            }
        }
    )
}

@Composable
private fun LocationSection(
    state: EntryState,
    onLocationTypeChange: (LocationType) -> Unit,
    onCaptureLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(title = "Location")

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission warning if not granted
                if (!state.locationPermissionGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Location permission required to capture coordinates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    AppButton(
                        text = "Grant Location Permission",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Location type selection (radio buttons)
                if (state.locationPermissionGranted) {
                    Text(
                        text = "Location Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Current Location option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.locationType == LocationType.CURRENT,
                                    onClick = { onLocationTypeChange(LocationType.CURRENT) }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.locationType == LocationType.CURRENT,
                                onClick = { onLocationTypeChange(LocationType.CURRENT) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Current Location",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Get real-time GPS coordinates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Last Known Location option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.locationType == LocationType.LAST_KNOWN,
                                    onClick = { onLocationTypeChange(LocationType.LAST_KNOWN) }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.locationType == LocationType.LAST_KNOWN,
                                onClick = { onLocationTypeChange(LocationType.LAST_KNOWN) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Last Known Location",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically uses previously recorded position",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Capture button - only show for current location type
                    if (state.locationType == LocationType.CURRENT) {
                        AppButton(
                            text = if (state.capturedLatitude != null) "Recapture Location" else "Capture Location",
                            onClick = onCaptureLocation,
                            enabled = !state.isCapturingLocation,
                            isLoading = state.isCapturingLocation,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Show loading for last known location
                    if (state.locationType == LocationType.LAST_KNOWN && state.isCapturingLocation) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Show captured coordinates
                    if (state.capturedLatitude != null && state.capturedLongitude != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Location Captured",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Latitude: ${String.format("%.6f", state.capturedLatitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Longitude: ${String.format("%.6f", state.capturedLongitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Show location error if any
                    state.locationError?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Note about optional location
                Text(
                    text = "Location is optional but recommended for better data analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
