package com.example.aquaforecast.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.aquaforecast.R
import com.example.aquaforecast.ui.components.AppCompactButton
import com.example.aquaforecast.ui.components.AppCompactOutlinedButton
import com.example.aquaforecast.ui.components.AppDialogButton
import com.example.aquaforecast.ui.components.AppErrorButton
import com.example.aquaforecast.ui.components.AppIconButton
import com.example.aquaforecast.ui.components.PondSelectorWithHeader
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDataEntry: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Refresh data when navigating to this screen
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.dashboard_title),
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

            state.availablePonds.isEmpty() -> {
                EmptyPondsState(
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            else -> {
                // Show pond selector and content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Pond selector - always visible when ponds exist
                    if (state.availablePonds.isNotEmpty()) {
                        PondSelectorWithHeader(
                            ponds = state.availablePonds,
                            selectedPond = state.selectedPond,
                            expanded = state.isPondDropdownExpanded,
                            onExpandedChange = viewModel::togglePondDropdown,
                            onPondSelected = viewModel::onPondSelected,
                            enabled = true,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Content area
                    when {
                        state.latestPrediction == null && state.latestFarmData == null -> {
                            NoDataState(
                                pondName = state.selectedPond?.name ?: "this pond",
                                onNavigateToDataEntry = onNavigateToDataEntry,
                                hasError = state.error != null,
                                errorMessage = state.error
                            )
                        }

                        else -> {
                            DashboardContent(
                                state = state,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Current Stats Overview
        state.latestPrediction?.let { prediction ->
            CurrentStatsCard(state = state, prediction = prediction)
        }

        // Growth Projections Chart
        if (state.growthProjections.isNotEmpty()) {
            GrowthProjectionsCard(state = state)
        }

        // Water Quality Summary
        state.latestFarmData?.let { farmData ->
            WaterQualitySummaryCard(farmData = farmData)
        }

        // Quick Actions
        QuickActionsCard()

        // Harvested badge
        if (state.pond?.isHarvested == true) {
            // Show harvested badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "This pond has been harvested and is now read-only. Manage from Pond Management.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    // Success Snackbar
    state.successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
        }
    }
}

@Composable
private fun CurrentStatsCard(
    state: DashboardState,
    prediction: com.example.aquaforecast.domain.model.Prediction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_current_stats),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weight
                StatItem(
                    icon = Icons.Outlined.MonitorWeight,
                    label = stringResource(R.string.dashboard_avg_weight),
                    value = prediction.getFormattedWeight(),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Length
                StatItem(
                    icon = Icons.Outlined.Straighten,
                    label = stringResource(R.string.dashboard_avg_length),
                    value = prediction.getFormattedLength(),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stock Count
                StatItem(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Stock Count",
                    value = "${state.pond?.stockCount ?: 0} fish",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Total Biomass
                StatItem(
                    icon = Icons.Outlined.Scale,
                    label = "Total Biomass",
                    value = "${"%.2f".format(prediction.calculateTotalYield(state.pond?.stockCount ?: 0))} kg",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            // Harvest Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Expected Harvest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = prediction.getFormattedHarvestDate(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (prediction.isHarvestReady)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "${prediction.daysUntilHarvest} days",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (prediction.isHarvestReady)
                            MaterialTheme.colorScheme.onTertiary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GrowthProjectionsCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Growth Projection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "+${state.weightChangePercent.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Next ${state.forecastHorizonDays} days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Growth Chart with gradient
            GrowthChartWithGradient(
                projections = state.growthProjections,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.2f".format(state.growthProjections.firstOrNull()?.weight ?: 0.0)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Projected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.2f".format(state.totalProjectedWeight)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthChartWithGradient(
    projections: List<GrowthProjection>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (projections.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f

        val maxWeight = projections.maxOfOrNull { it.weight } ?: 1.0
        val minWeight = projections.minOfOrNull { it.weight } ?: 0.0
        val weightRange = (maxWeight - minWeight).coerceAtLeast(0.01)

        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        // Draw grid lines
        for (i in 0..4) {
            val y = padding + (chartHeight / 4) * i
            drawLine(
                color = surfaceColor.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.5f
            )
        }

        // Build line path
        val linePath = Path()
        val fillPath = Path()

        projections.forEachIndexed { index, projection ->
            val x = padding + (chartWidth / (projections.size - 1).coerceAtLeast(1)) * index
            val normalizedWeight = ((projection.weight - minWeight) / weightRange).toFloat()
            val y = height - padding - (chartHeight * normalizedWeight)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        val lastX = padding + chartWidth
        fillPath.lineTo(lastX, height - padding)
        fillPath.close()

        // Draw gradient fill
        val gradientColors = listOf(
            primaryColor.copy(alpha = 0.3f),
            primaryColor.copy(alpha = 0.05f),
            Color.Transparent
        )

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = padding,
                endY = height - padding
            )
        )

        // Draw line
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(width = 4f)
        )

        // Draw data points
        projections.forEachIndexed { index, projection ->
            val x = padding + (chartWidth / (projections.size - 1).coerceAtLeast(1)) * index
            val normalizedWeight = ((projection.weight - minWeight) / weightRange).toFloat()
            val y = height - padding - (chartHeight * normalizedWeight)

            // Outer circle
            drawCircle(
                color = primaryColor,
                radius = 6f,
                center = Offset(x, y)
            )
            // Inner circle
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun WaterQualitySummaryCard(farmData: com.example.aquaforecast.domain.model.FarmData) {
    val status = farmData.getOverallStatus()
    val statusColor = when (status) {
        com.example.aquaforecast.domain.model.WaterQualityStatus.OPTIMAL -> Color(0xFF4CAF50)
        com.example.aquaforecast.domain.model.WaterQualityStatus.WARNING -> Color(0xFFFFA726)
        com.example.aquaforecast.domain.model.WaterQualityStatus.CRITICAL -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Water Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (status) {
                                com.example.aquaforecast.domain.model.WaterQualityStatus.OPTIMAL -> "Optimal"
                                com.example.aquaforecast.domain.model.WaterQualityStatus.WARNING -> "Warning"
                                com.example.aquaforecast.domain.model.WaterQualityStatus.CRITICAL -> "Critical"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Parameters grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WaterParameterItem(
                        label = "Temperature",
                        value = "${"%.2f".format(farmData.temperature)}°C",
                        modifier = Modifier.weight(1f)
                    )
                    WaterParameterItem(
                        label = "pH",
                        value = "%.2f".format(farmData.ph),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WaterParameterItem(
                        label = "Dissolved O₂",
                        value = "${"%.2f".format(farmData.dissolvedOxygen)} mg/L",
                        modifier = Modifier.weight(1f)
                    )
                    WaterParameterItem(
                        label = "Ammonia",
                        value = "${"%.2f".format(farmData.ammonia)} mg/L",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterParameterItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Predictions are updated daily based on your water quality data and feeding schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyPondsState(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            append("Please go to ")
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
            append(" to configure your pond to start viewing predictions.")
        }

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoDataState(
    pondName: String,
    onNavigateToDataEntry: () -> Unit,
    hasError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasError) Icons.Outlined.Error else Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (hasError)
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasError) "Error Loading Data" else "No Data Available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (hasError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        val annotatedText = buildAnnotatedString {
            append("Add water quality data for ")
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(pondName)
            }
            append(" to see growth predictions. Visit the ")
            withLink(
                LinkAnnotation.Clickable(
                    "data_entry",
                    TextLinkStyles(
                        style = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.W400,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    {
                        onNavigateToDataEntry()
                    }
                )
            ) {
                append("Data Entry screen")
            }
            append(" to get started.")
        }

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtonsCard(
    onReportDeath: () -> Unit,
    onMarkHarvested: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pond Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppCompactOutlinedButton(
                    text = "Report Deaths",
                    onClick = onReportDeath,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Warning,
                    contentColor = MaterialTheme.colorScheme.error,
                    borderColor = MaterialTheme.colorScheme.error
                )

                AppCompactButton(
                    text = "Mark Harvested",
                    onClick = onMarkHarvested,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDeathDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var deathCount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Report Fish Deaths",
                    style = MaterialTheme.typography.titleLarge,
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

                OutlinedTextField(
                    value = deathCount,
                    onValueChange = {
                        deathCount = it
                        error = null
                    },
                    label = { Text("Number of Deaths") },
                    placeholder = { Text("Enter number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            AppErrorButton(
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

