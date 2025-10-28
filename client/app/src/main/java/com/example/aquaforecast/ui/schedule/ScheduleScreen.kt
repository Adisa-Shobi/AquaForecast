package com.example.aquaforecast.ui.schedule

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.domain.model.FeedingSchedule
import com.example.aquaforecast.ui.components.AppDialogButton
import com.example.aquaforecast.ui.components.AppFloatingActionButton
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.AppTextButton
import com.example.aquaforecast.ui.components.EmptyState
import com.example.aquaforecast.ui.components.LoadingState
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime

private const val TAG = "ScheduleScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToCreateSchedule: () -> Unit = {},
    onNavigateToEditSchedule: (Long) -> Unit = {},
    viewModel: ScheduleViewModel
) {
    val state by viewModel.state.collectAsState()

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result is handled, notifications will work if granted
    }

    // Request permission on first composition if needed
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Feeding Schedules",
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
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                state.isLoading -> {
                    LoadingState(
                        message = "Loading schedules..."
                    )
                }

                state.schedules.isEmpty() -> {
                    EmptyState(
                        title = "No Schedules Yet",
                        message = "Create a feeding schedule to receive reminders for feeding your fish",
                        actionText = "Create Schedule",
                        onActionClick = {
                            viewModel.prepareCreateSchedule()
                            onNavigateToCreateSchedule()
                        }
                    )
                }

                else -> {
                    ScheduleList(
                        schedules = state.schedules,
                        onEditSchedule = { schedule ->
                            viewModel.prepareEditSchedule(schedule)
                            onNavigateToEditSchedule(schedule.id)
                        },
                        onDeleteSchedule = { viewModel.deleteSchedule(it.id) },
                        onToggleActive = { schedule -> viewModel.toggleScheduleActive(schedule.id, schedule.isActive) }
                    )
                }
            }

            // Show error snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        AppTextButton(
                            text = "Dismiss",
                            onClick = { viewModel.clearError() }
                        )
                    }
                ) {
                    Text(error)
                }
            }

            // Show success snackbar
            state.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(message)
                }
            }
            }
        }

        // Floating Action Button
        AppFloatingActionButton(
            icon = Icons.Default.Add,
            onClick = {
                viewModel.prepareCreateSchedule()
                onNavigateToCreateSchedule()
            },
            contentDescription = "Add Schedule",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun ScheduleList(
    schedules: List<FeedingSchedule>,
    onEditSchedule: (FeedingSchedule) -> Unit,
    onDeleteSchedule: (FeedingSchedule) -> Unit,
    onToggleActive: (FeedingSchedule) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleCard(
                schedule = schedule,
                onEdit = { onEditSchedule(schedule) },
                onDelete = { onDeleteSchedule(schedule) },
                onToggleActive = { onToggleActive(schedule) }
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: FeedingSchedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Content with padding
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with schedule name and active status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Title
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Active/Inactive badge
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (schedule.isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (schedule.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (schedule.isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Pond name
                Text(
                    text = schedule.pondName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                // Subtitle: Time and frequency
                Text(
                    text = "${schedule.getFormattedTime()}, Daily",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Days remaining or days until start
                when {
                    schedule.hasNotStarted() -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timelapse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Starts in ${schedule.getDaysUntilStart()} days",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    schedule.isCurrentlyActive() -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timelapse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${schedule.getRemainingDays()} days remaining",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Full width divider (no padding)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Action buttons with padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                AppIconButton(
                    icon = Icons.Outlined.Edit,
                    onClick = onEdit,
                    contentDescription = "Edit Schedule",
                    tint = MaterialTheme.colorScheme.onSurface
                )

                // Delete button
                AppIconButton(
                    icon = Icons.Outlined.Delete,
                    onClick = { showDeleteConfirmation = true },
                    contentDescription = "Delete Schedule",
                    tint = MaterialTheme.colorScheme.onSurface
                )

                // Toggle notification button
                AppIconButton(
                    icon = if (schedule.isActive) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                    onClick = onToggleActive,
                    contentDescription = if (schedule.isActive) "Turn off notifications" else "Turn on notifications",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Schedule") },
            text = { Text("Are you sure you want to delete this feeding schedule?") },
            confirmButton = {
                AppTextButton(
                    text = "Delete",
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                )
            },
            dismissButton = {
                AppDialogButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirmation = false },
                    isPrimary = false
                )
            }
        )
    }
}
