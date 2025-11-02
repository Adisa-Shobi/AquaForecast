package com.example.aquaforecast.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.aquaforecast.domain.model.Pond

/**
 * Reusable pond selection dropdown component
 * Displays pond name with species and stock count details
 * Used across Dashboard, Data Entry, and Schedule screens
 */
@Composable
fun PondSelector(
    ponds: List<Pond>,
    selectedPond: Pond?,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onPondSelected: (Pond) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDetails: Boolean = true
) {
    AppDropdownField(
        value = selectedPond?.name ?: "",
        placeholder = "Select a pond",
        label = "Pond",
        icon = Icons.Default.ArrowDropDown,
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange() },
        modifier = modifier
    ) {
        ponds.forEach { pond ->
            DropdownMenuItem(
                text = {
                    if (showDetails) {
                        Column {
                            Text(pond.name)
                            Text(
                                text = buildString {
                                    append("${pond.species.displayName} • ${pond.stockCount} fish")
                                    if (pond.isHarvested) {
                                        append(" • Harvested")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pond.isHarvested) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    } else {
                        Text(pond.name)
                    }
                },
                onClick = {
                    onPondSelected(pond)
                }
            )
        }
    }
}

/**
 * Pond selector with section header
 * Commonly used pattern across screens
 */
@Composable
fun PondSelectorWithHeader(
    ponds: List<Pond>,
    selectedPond: Pond?,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onPondSelected: (Pond) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDetails: Boolean = true,
    headerTitle: String = "Select Pond"
) {
    Column(modifier = modifier) {
        SectionHeader(title = headerTitle)

        PondSelector(
            ponds = ponds,
            selectedPond = selectedPond,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onPondSelected = onPondSelected,
            enabled = enabled,
            showDetails = showDetails
        )
    }
}
