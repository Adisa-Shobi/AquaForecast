package com.example.aquaforecast.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.WaterQualityStatus
import com.example.aquaforecast.ui.components.IconCard
import com.example.aquaforecast.ui.components.SectionHeader

@Composable
fun PondStatusSection(
    ponds: List<PondStatus>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Pond Status")

        Spacer(modifier = Modifier.height(16.dp))

        ponds.forEach { pondStatus ->
            IconCard(
                icon = Icons.Default.WaterDrop,
                title = pondStatus.pondName,
                subtitle = pondStatus.statusText,
                subtitleColor = pondStatus.statusColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

data class PondStatus(
    val pondName: String,
    val statusText: String,
    val statusColor: Color,
    val waterQualityStatus: WaterQualityStatus
)

fun createPondStatus(pond: Pond, farmData: FarmData?): PondStatus {
    val status = farmData?.getOverallStatus() ?: WaterQualityStatus.WARNING
    val statusText = when (status) {
        WaterQualityStatus.OPTIMAL -> "Healthy"
        WaterQualityStatus.WARNING -> "Needs Attention"
        WaterQualityStatus.CRITICAL -> "Critical"
    }

    return PondStatus(
        pondName = pond.name,
        statusText = statusText,
        statusColor = status.getColor(),
        waterQualityStatus = status
    )
}
