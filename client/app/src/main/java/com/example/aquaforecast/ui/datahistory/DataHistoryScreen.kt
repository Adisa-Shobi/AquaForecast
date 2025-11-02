package com.example.aquaforecast.ui.datahistory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.WaterQualityStatus
import com.example.aquaforecast.ui.components.AppDialogButton
import com.example.aquaforecast.ui.components.AppOutlinedTextField
import com.example.aquaforecast.ui.components.EmptyState
import com.example.aquaforecast.ui.components.LoadingState
import com.example.aquaforecast.ui.components.PondSelectorWithHeader
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataHistoryScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDataEntry: () -> Unit = {},
    viewModel: DataHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Refresh ponds on navigation
    LaunchedEffect(Unit) {
        viewModel.loadPonds()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Data History",
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
                state.isLoading && state.farmDataList.isEmpty() -> {
                    LoadingState(message = "Loading data...")
                }

                state.availablePonds.isEmpty() -> {
                    NoPondsState(
                        onNavigateToSettings = onNavigateToSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Pond Selector
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (state.availablePonds.size > 1) {
                                PondSelectorWithHeader(
                                    ponds = state.availablePonds,
                                    selectedPond = state.selectedPond,
                                    expanded = state.isPondDropdownExpanded,
                                    onExpandedChange = viewModel::togglePondDropdown,
                                    onPondSelected = viewModel::onPondSelected
                                )
                            } else {
                                // Show single pond info
                                state.selectedPond?.let { pond ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = pond.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "${pond.species.displayName} • ${pond.stockCount} fish",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactTab(
                                text = "History",
                                selected = state.selectedTab == DataHistoryTab.LIST,
                                onClick = { viewModel.selectTab(DataHistoryTab.LIST) },
                                modifier = Modifier.weight(1f)
                            )
                            CompactTab(
                                text = "Trends",
                                selected = state.selectedTab == DataHistoryTab.TRENDS,
                                onClick = { viewModel.selectTab(DataHistoryTab.TRENDS) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Tab Content
                        when (state.selectedTab) {
                            DataHistoryTab.LIST -> {
                                DataListView(
                                    farmDataList = state.farmDataList,
                                    pondName = state.selectedPond?.name ?: "this pond",
                                    onEdit = viewModel::openEditDialog,
                                    onNavigateToDataEntry = onNavigateToDataEntry
                                )
                            }
                            DataHistoryTab.TRENDS -> {
                                TrendsView(
                                    farmDataList = state.farmDataList,
                                    pondName = state.selectedPond?.name ?: "this pond",
                                    onNavigateToDataEntry = onNavigateToDataEntry
                                )
                            }
                        }
                    }
                }
            }

            // Error Snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            }
        }

        // Edit Dialog
        if (state.isEditDialogOpen && state.editingData != null) {
            EditDataDialog(
                farmData = state.editingData!!,
                onDismiss = { viewModel.closeEditDialog() },
                onSave = { updatedData -> viewModel.updateFarmData(updatedData) }
            )
        }
    }
}

@Composable
private fun CompactTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DataListView(
    farmDataList: List<FarmData>,
    pondName: String,
    onEdit: (FarmData) -> Unit,
    onNavigateToDataEntry: () -> Unit
) {
    when {
        farmDataList.isEmpty() -> {
            NoDataState(
                pondName = pondName,
                onNavigateToDataEntry = onNavigateToDataEntry,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(farmDataList, key = { it.id }) { farmData ->
                    FarmDataCard(
                        farmData = farmData,
                        onEdit = { onEdit(farmData) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendsView(
    farmDataList: List<FarmData>,
    pondName: String,
    onNavigateToDataEntry: () -> Unit
) {
    when {
        farmDataList.isEmpty() -> {
            NoDataState(
                pondName = pondName,
                onNavigateToDataEntry = onNavigateToDataEntry,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Temperature Trend
                TrendChartCard(
                    title = "Temperature (°C)",
                    data = farmDataList.map { it.temperature },
                    timestamps = farmDataList.map { it.timestamp },
                    color = MaterialTheme.colorScheme.primary
                )

                // pH Trend
                TrendChartCard(
                    title = "pH",
                    data = farmDataList.map { it.ph },
                    timestamps = farmDataList.map { it.timestamp },
                    color = MaterialTheme.colorScheme.secondary
                )

                // Dissolved Oxygen Trend
                TrendChartCard(
                    title = "Dissolved Oxygen (mg/L)",
                    data = farmDataList.map { it.dissolvedOxygen },
                    timestamps = farmDataList.map { it.timestamp },
                    color = MaterialTheme.colorScheme.tertiary
                )

                // Ammonia Trend
                TrendChartCard(
                    title = "Ammonia (mg/L)",
                    data = farmDataList.map { it.ammonia },
                    timestamps = farmDataList.map { it.timestamp },
                    color = MaterialTheme.colorScheme.error
                )

                // Nitrate Trend
                TrendChartCard(
                    title = "Nitrate (mg/L)",
                    data = farmDataList.map { it.nitrate },
                    timestamps = farmDataList.map { it.timestamp },
                    color = Color(0xFF9C27B0) // Purple
                )

                // Turbidity Trend
                TrendChartCard(
                    title = "Turbidity (NTU)",
                    data = farmDataList.map { it.turbidity },
                    timestamps = farmDataList.map { it.timestamp },
                    color = Color(0xFFFF9800) // Orange
                )

                // Bottom padding for navigation bar
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TrendChartCard(
    title: String,
    data: List<Double>,
    timestamps: List<Long>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Chart
            TrendChart(
                data = data,
                timestamps = timestamps,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Min",
                    value = "%.2f".format(data.minOrNull() ?: 0.0),
                    color = color
                )
                StatItem(
                    label = "Avg",
                    value = "%.2f".format(data.average()),
                    color = color
                )
                StatItem(
                    label = "Max",
                    value = "%.2f".format(data.maxOrNull() ?: 0.0),
                    color = color
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TrendChart(
    data: List<Double>,
    timestamps: List<Long>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f

        val maxValue = data.maxOrNull() ?: 1.0
        val minValue = data.minOrNull() ?: 0.0
        val valueRange = (maxValue - minValue).coerceAtLeast(0.01)

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

        data.forEachIndexed { index, value ->
            val x = padding + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
            val normalizedValue = ((value - minValue) / valueRange).toFloat()
            val y = height - padding - (chartHeight * normalizedValue)

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
            color.copy(alpha = 0.3f),
            color.copy(alpha = 0.05f),
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
            color = color,
            style = Stroke(width = 4f)
        )

        // Draw data points (only show every nth point if too many)
        val step = if (data.size > 20) data.size / 20 else 1
        data.forEachIndexed { index, value ->
            if (index % step == 0 || index == data.size - 1) {
                val x = padding + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
                val normalizedValue = ((value - minValue) / valueRange).toFloat()
                val y = height - padding - (chartHeight * normalizedValue)

                // Outer circle
                drawCircle(
                    color = color,
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
}

@Composable
fun FarmDataCard(
    farmData: FarmData,
    onEdit: () -> Unit
) {
    val status = farmData.getOverallStatus()
    val statusColor = when (status) {
        WaterQualityStatus.OPTIMAL -> MaterialTheme.colorScheme.primary
        WaterQualityStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        WaterQualityStatus.CRITICAL -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with date and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = farmData.getFormattedDate(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            WaterQualityStatus.OPTIMAL -> "Optimal"
                            WaterQualityStatus.WARNING -> "Warning"
                            WaterQualityStatus.CRITICAL -> "Critical"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Parameters Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParameterItem(
                        label = "Temperature",
                        value = "${"%.2f".format(farmData.temperature)}°C",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterItem(
                        label = "pH",
                        value = "%.2f".format(farmData.ph),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParameterItem(
                        label = "Dissolved O₂",
                        value = "${"%.2f".format(farmData.dissolvedOxygen)} mg/L",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterItem(
                        label = "Ammonia",
                        value = "${"%.2f".format(farmData.ammonia)} mg/L",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParameterItem(
                        label = "Nitrate",
                        value = "${"%.2f".format(farmData.nitrate)} mg/L",
                        modifier = Modifier.weight(1f)
                    )
                    ParameterItem(
                        label = "Turbidity",
                        value = "${"%.2f".format(farmData.turbidity)} NTU",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ParameterItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDataDialog(
    farmData: FarmData,
    onDismiss: () -> Unit,
    onSave: (FarmData) -> Unit
) {
    var temperature by remember { mutableStateOf(farmData.temperature.toString()) }
    var ph by remember { mutableStateOf(farmData.ph.toString()) }
    var dissolvedOxygen by remember { mutableStateOf(farmData.dissolvedOxygen.toString()) }
    var ammonia by remember { mutableStateOf(farmData.ammonia.toString()) }
    var nitrate by remember { mutableStateOf(farmData.nitrate.toString()) }
    var turbidity by remember { mutableStateOf(farmData.turbidity.toString()) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Water Quality Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AppOutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = "Temperature (°C)",
                        placeholder = "Enter temperature",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = ph,
                        onValueChange = { ph = it },
                        label = "pH",
                        placeholder = "Enter pH value",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = dissolvedOxygen,
                        onValueChange = { dissolvedOxygen = it },
                        label = "Dissolved Oxygen (mg/L)",
                        placeholder = "Enter value",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = ammonia,
                        onValueChange = { ammonia = it },
                        label = "Ammonia (mg/L)",
                        placeholder = "Enter value",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = nitrate,
                        onValueChange = { nitrate = it },
                        label = "Nitrate (mg/L)",
                        placeholder = "Enter value",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    AppOutlinedTextField(
                        value = turbidity,
                        onValueChange = { turbidity = it },
                        label = "Turbidity (NTU)",
                        placeholder = "Enter value",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            AppDialogButton(
                text = "Save",
                onClick = {
                    try {
                        val updatedData = farmData.copy(
                            temperature = temperature.toDouble(),
                            ph = ph.toDouble(),
                            dissolvedOxygen = dissolvedOxygen.toDouble(),
                            ammonia = ammonia.toDouble(),
                            nitrate = nitrate.toDouble(),
                            turbidity = turbidity.toDouble()
                        )
                        onSave(updatedData)
                    } catch (e: NumberFormatException) {
                        // Handle invalid input
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
private fun NoPondsState(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Ponds Available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        val annotatedText = buildAnnotatedString {
            append("Create a pond in ")
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
                append("Settings")
            }
            append(" to start tracking water quality data.")
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Data Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        val annotatedText = buildAnnotatedString {
            append("Start entering water quality data for ")
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(pondName)
            }
            append(" to see your history here. Visit the ")
            withLink(
                LinkAnnotation.Clickable(
                    "data_entry",
                    TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = FontStyle.Italic
                        )
                    ),
                    { onNavigateToDataEntry() }
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
