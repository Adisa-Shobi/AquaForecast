package com.example.aquaforecast.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reusable Date Picker Dialog Component
 * Provides consistent date picking experience across the app
 *
 * @param selectedDate The currently selected date
 * @param onDateSelected Callback when a date is selected
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay()
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Reusable Time Picker Dialog Component
 * Provides consistent time picking experience across the app
 *
 * @param selectedTime The currently selected time
 * @param onTimeSelected Callback when a time is selected
 * @param onDismiss Callback when dialog is dismissed
 * @param is24Hour Whether to use 24-hour format (default: false)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    is24Hour: Boolean = false
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime?.hour ?: 7,
        initialMinute = selectedTime?.minute ?: 0,
        is24Hour = is24Hour
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onTimeSelected(time)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Date Picker Field with integrated dialog
 * Combines the date field and picker dialog into a single component
 *
 * @param value The formatted date string to display
 * @param selectedDate The LocalDate value
 * @param onDateSelected Callback when a date is selected
 * @param label Label for the field
 * @param placeholder Placeholder text
 * @param icon Icon to display
 * @param modifier Modifier for the component
 */
@Composable
fun AppDatePickerField(
    value: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    AppDateField(
        value = value,
        label = label,
        placeholder = placeholder,
        icon = icon,
        onClick = { showPicker = true },
        modifier = modifier
    )

    if (showPicker) {
        AppDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Time Picker Field with integrated dialog
 * Combines the time field and picker dialog into a single component
 *
 * @param value The formatted time string to display
 * @param selectedTime The LocalTime value
 * @param onTimeSelected Callback when a time is selected
 * @param label Label for the field
 * @param placeholder Placeholder text
 * @param icon Icon to display
 * @param is24Hour Whether to use 24-hour format
 * @param modifier Modifier for the component
 */
@Composable
fun AppTimePickerField(
    value: String,
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    is24Hour: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    AppTimeField(
        value = value,
        label = label,
        placeholder = placeholder,
        icon = icon,
        onClick = { showPicker = true },
        modifier = modifier
    )

    if (showPicker) {
        AppTimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = onTimeSelected,
            onDismiss = { showPicker = false },
            is24Hour = is24Hour
        )
    }
}
