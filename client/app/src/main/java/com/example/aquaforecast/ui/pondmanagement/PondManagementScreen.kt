package com.example.aquaforecast.ui.pondmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.ui.components.AppButton
import com.example.aquaforecast.ui.components.AppDialogButton
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.AppTextButton
import com.example.aquaforecast.ui.components.ErrorState
import com.example.aquaforecast.ui.components.LoadingState
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PondManagementScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToCreatePond: () -> Unit = {},
    onNavigateToEditPond: (Long) -> Unit = {},
    viewModel: PondManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Manage Ponds",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    AppIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    AppIconButton(
                        icon = Icons.Default.Add,
                        onClick = onNavigateToCreatePond,
                        contentDescription = "Add Pond"
                    )
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues),
                    message = "Loading ponds..."
                )
            }

            state.error != null -> {
                ErrorState(
                    message = state.error ?: "Unknown error",
                    onActionClick = { viewModel.loadPonds() },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            state.ponds.isEmpty() -> {
                EmptyPondsState(
                    onAddPond = onNavigateToCreatePond,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                PondList(
                    ponds = state.ponds,
                    onEditPond = onNavigateToEditPond,
                    onDeletePond = viewModel::showDeleteDialog,
                    onToggleHarvest = viewModel::togglePondHarvest,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Delete confirmation dialog
        if (state.showDeleteDialog && state.pondToDelete != null) {
            DeletePondDialog(
                pond = state.pondToDelete!!,
                onConfirm = viewModel::deletePond,
                onDismiss = viewModel::dismissDeleteDialog
            )
        }
    }
}

@Composable
private fun EmptyPondsState(
    onAddPond: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "No Ponds Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your first pond to start tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppButton(
                text = "Add Pond",
                onClick = onAddPond,
                icon = Icons.Default.Add
            )
        }
    }
}

@Composable
private fun PondList(
    ponds: List<Pond>,
    onEditPond: (Long) -> Unit,
    onDeletePond: (Pond) -> Unit,
    onToggleHarvest: (Pond) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ponds, key = { it.id }) { pond ->
            PondCard(
                pond = pond,
                onEdit = { onEditPond(pond.id) },
                onDelete = { onDeletePond(pond) },
                onToggleHarvest = onToggleHarvest
            )
        }
    }
}

@Composable
private fun PondCard(
    pond: Pond,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHarvest: (Pond) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pond.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIconButton(
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AppIconButton(
                        icon = Icons.Default.Delete,
                        onClick = onDelete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "Species", value = pond.species.name.lowercase().replaceFirstChar { it.uppercase() })
                InfoItem(label = "Stock Count", value = "${pond.stockCount}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Start Date",
                    value = pond.startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                )
                InfoItem(
                    label = "Status",
                    value = if (pond.isHarvested) "Harvested" else "Active"
                )
            }

            // Harvest toggle button
            HorizontalDivider()

            AppButton(
                text = if (pond.isHarvested) "Mark as Not Harvested" else "Mark as Harvested",
                onClick = { onToggleHarvest(pond) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeletePondDialog(
    pond: Pond,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Pond?") },
        text = {
            Text("Are you sure you want to delete '${pond.name}'? This will remove all associated data and cannot be undone.")
        },
        confirmButton = {
            AppTextButton(
                text = "Delete",
                onClick = onConfirm
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
