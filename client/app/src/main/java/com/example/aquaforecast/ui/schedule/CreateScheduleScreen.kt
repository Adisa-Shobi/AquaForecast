package com.example.aquaforecast.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.ui.components.AppButton
import com.example.aquaforecast.ui.components.AppDatePickerField
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.AppOutlinedTextField
import com.example.aquaforecast.ui.components.AppTextButton
import com.example.aquaforecast.ui.components.AppTimePickerField
import com.example.aquaforecast.ui.components.PondSelector
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScheduleViewModel
) {
    val state by viewModel.state.collectAsState()

    var showPondDropdown by remember { mutableStateOf(false) }

    // Refresh ponds when navigating to this screen
    LaunchedEffect(Unit) {
        viewModel.refreshPonds()
    }

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
                        text = if (state.isEditMode) "Edit Feeding Schedule" else "New Feeding Schedule",
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
                        onClick = { viewModel.saveSchedule() },
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Schedule Name Input
            AppOutlinedTextField(
                value = state.selectedName ?: "",
                onValueChange = { viewModel.onNameChanged(it) },
                placeholder = "e.g., Morning Feed",
                label = "Schedule Name"
            )

            // Pond Selection
            PondSelector(
                ponds = state.availablePonds,
                selectedPond = state.availablePonds.firstOrNull { it.name == state.selectedPondName },
                expanded = showPondDropdown,
                onExpandedChange = { showPondDropdown = !showPondDropdown },
                onPondSelected = { pond ->
                    viewModel.onPondSelected(pond)
                    showPondDropdown = false
                },
                showDetails = false
            )

            // Schedule Duration
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Schedule Duration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date
                    AppDatePickerField(
                        value = state.selectedStartDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                        selectedDate = state.selectedStartDate,
                        onDateSelected = { viewModel.onStartDateSelected(it) },
                        label = "Start Date",
                        placeholder = "Select a date",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )

                    // End Date
                    AppDatePickerField(
                        value = state.selectedEndDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                        selectedDate = state.selectedEndDate,
                        onDateSelected = { viewModel.onEndDateSelected(it) },
                        label = "End Date",
                        placeholder = "Select a date",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Feeding Time
            AppTimePickerField(
                value = state.selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "",
                selectedTime = state.selectedTime,
                onTimeSelected = { viewModel.onTimeSelected(it) },
                label = "Daily Feeding Time",
                placeholder = "Tap to set time",
                icon = Icons.Default.AccessTime
            )

            // Error message
            state.dialogError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            AppButton(
                text = if (state.isEditMode) "Update Schedule" else "Save Schedule",
                onClick = { viewModel.saveSchedule() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                isLoading = state.isSaving
            )
        }
    }
}
