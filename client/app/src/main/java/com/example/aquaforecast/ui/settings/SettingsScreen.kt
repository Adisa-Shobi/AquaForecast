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
import com.example.aquaforecast.ui.components.FilledTextField
import com.example.aquaforecast.ui.components.SectionHeader
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "SettingsScreen"

/**
 * Settings screen matching HTML design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
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
                        text = "Farm Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Pond Details Section
                SectionHeader(title = "Pond Details")

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pond Name
                        FilledTextField(
                            value = state.pondName,
                            onValueChange = viewModel::onPondNameChanged,
                            placeholder = "Pond Name",
                            isError = state.pondNameError != null,
                            errorMessage = state.pondNameError,
                            enabled = !state.isSaving
                        )

                        // Species Dropdown
                        ExposedDropdownMenuBox(
                            expanded = state.isSpeciesDropdownExpanded,
                            onExpandedChange = {
                                if (!state.isSaving) viewModel.toggleSpeciesDropdown()
                            }
                        ) {
                            FilledTextField(
                                value = state.species.ifBlank { "Fish Type" },
                                onValueChange = {},
                                placeholder = "Fish Type",
                                readOnly = true,
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                },
                                isError = state.speciesError != null,
                                errorMessage = state.speciesError,
                                enabled = !state.isSaving,
                                modifier = Modifier.menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = state.isSpeciesDropdownExpanded,
                                onDismissRequest = viewModel::dismissSpeciesDropdown
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tilapia") },
                                    onClick = {
                                        viewModel.onSpeciesChanged("Tilapia")
                                        viewModel.dismissSpeciesDropdown()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Catfish") },
                                    onClick = {
                                        viewModel.onSpeciesChanged("Catfish")
                                        viewModel.dismissSpeciesDropdown()
                                    }
                                )
                            }
                        }

                        // Fish Count
                        FilledTextField(
                            value = state.stockCount,
                            onValueChange = viewModel::onStockCountChanged,
                            placeholder = "Fish Count",
                            isError = state.stockCountError != null,
                            errorMessage = state.stockCountError,
                            enabled = !state.isSaving
                        )

                        // Start Date
                        FilledTextField(
                            value = state.startDate?.format(
                                DateTimeFormatter.ofPattern("MMM dd, yyyy")
                            ) ?: "",
                            onValueChange = {},
                            placeholder = "Start Date",
                            readOnly = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (!state.isSaving) viewModel.toggleDatePicker()
                                    }
                                ) {
                                    Icon(Icons.Default.CalendarToday, "Select date")
                                }
                            },
                            isError = state.startDateError != null,
                            errorMessage = state.startDateError,
                            enabled = !state.isSaving
                        )
                    }
                }
                //
                SectionHeader(title = "Forecast Options")
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledTextField(
                            value = state.forecastHorizon,
                            onValueChange = viewModel::onForecastHorizonChanged,
                            placeholder = "Forecast Horizon (days)",
                            isError = state.forecastHorizonError != null,
                            errorMessage = state.forecastHorizonError,
                            enabled = !state.isSaving
                        )
                    }}


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
                            Text(
                                text = "Sync Data",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(
                                onClick = viewModel::syncData,
                                enabled = !state.isSyncing
                            ) {
                                if (state.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                            Text(
                                text = "Offline Mode",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Switch(
                                checked = state.isOfflineMode,
                                onCheckedChange = { viewModel.toggleOfflineMode() }
                            )
                        }
                    }
                }

                // Account Section
                SectionHeader(title = "Account")

                AppButton(
                    text = "Sign Out",
                    onClick = viewModel::signOut,
                    modifier = Modifier.fillMaxWidth()
                )

                // Save button for pond configuration
                if (state.pondName.isNotBlank() || state.species.isNotBlank() || state.stockCount.isNotBlank()) {
                    AppButton(
                        text = "Save Configuration",
                        onClick = viewModel::savePondConfig,
                        enabled = !state.isSaving,
                        isLoading = state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

        // Date Picker Dialog
        if (state.showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.startDate
                    ?.atTime(12, 0)
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()
            )

            DatePickerDialog(
                onDismissRequest = viewModel::dismissDatePicker,
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                viewModel.onStartDateChanged(millis)
                            }
                            viewModel.dismissDatePicker()
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDatePicker) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}