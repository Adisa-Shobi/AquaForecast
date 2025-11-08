package com.example.aquaforecast.ui.pondmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.AppDatePickerDialog
import com.example.aquaforecast.ui.components.AppDropdownField
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.AppOutlinedTextField
import com.example.aquaforecast.ui.components.AppTextButton
import com.example.aquaforecast.ui.components.SectionHeader
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePondScreen(
    onNavigateBack: () -> Unit,
    viewModel: PondFormViewModel
) {
    val state by viewModel.state.collectAsState()

    // Navigate back on successful save
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Pond" else "New Pond",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    AppIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    AppTextButton(
                        text = "Save",
                        onClick = { viewModel.savePond() },
                        enabled = !state.isSaving
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pond Details Section
            SectionHeader(title = "Pond Details")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pond Name
                    AppOutlinedTextField(
                        value = state.pondName,
                        onValueChange = viewModel::onPondNameChanged,
                        placeholder = "Enter pond name",
                        label = "Pond Name",
                        enabled = !state.isSaving
                    )

                    // Species Dropdown
                    AppDropdownField(
                        value = state.species,
                        placeholder = "Select fish species",
                        label = "Fish Species",
                        icon = Icons.Default.ArrowDropDown,
                        expanded = state.isSpeciesDropdownExpanded,
                        onExpandedChange = {
                            if (!state.isSaving) viewModel.toggleSpeciesDropdown()
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("African Catfish") },
                            onClick = {
                                viewModel.onSpeciesChanged("CATFISH")
                                viewModel.dismissSpeciesDropdown()
                            }
                        )
                    }

                    // Stock Count
                    AppOutlinedTextField(
                        value = state.stockCount,
                        onValueChange = viewModel::onStockCountChanged,
                        placeholder = "Enter initial fish count",
                        label = "Stock Count",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        enabled = !state.isSaving
                    )

                    // Start Date
                    AppOutlinedTextField(
                        value = state.startDate?.format(
                            DateTimeFormatter.ofPattern("MMM dd, yyyy")
                        ) ?: "",
                        onValueChange = {},
                        placeholder = "Select start date",
                        label = "Start Date",
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarToday, "Select date")
                        },
                        enabled = !state.isSaving,
                        onClickable = {
                            if (!state.isSaving) viewModel.toggleDatePicker()
                        }
                    )
                }
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

        // Date Picker Dialog
        if (state.showDatePicker) {
            AppDatePickerDialog(
                selectedDate = state.startDate,
                onDateSelected = { localDate ->
                    // Convert LocalDate to millis for ViewModel
                    val millis = localDate.atTime(12, 0)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    viewModel.onStartDateChanged(millis)
                },
                onDismiss = viewModel::dismissDatePicker
            )
        }
    }
}
