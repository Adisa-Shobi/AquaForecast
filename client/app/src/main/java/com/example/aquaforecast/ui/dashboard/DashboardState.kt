package com.example.aquaforecast.ui.dashboard

import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Prediction

data class DashboardState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pond: Pond? = null,
    val availablePonds: List<Pond> = emptyList(),
    val selectedPond: Pond? = null,
    val isPondDropdownExpanded: Boolean = false,
    val latestPrediction: Prediction? = null,
    val latestFarmData: FarmData? = null,
    val growthProjections: List<GrowthProjection> = emptyList(),
    val isRefreshing: Boolean = false,
    val isOfflineMode: Boolean = false,
    val forecastHorizonDays: Int = 20,
    val showReportDeathDialog: Boolean = false,
    val showHarvestDialog: Boolean = false,
    val successMessage: String? = null
) {
    val hasData: Boolean
        get() = pond != null && growthProjections.isNotEmpty()

    val showEmptyState: Boolean
        get() = !isLoading && !hasData && error == null

    val totalProjectedWeight: Double
        get() = growthProjections.lastOrNull()?.weight ?: 0.0

    val weightChangePercent: Double
        get() {
            val first = growthProjections.firstOrNull()?.weight ?: 0.0
            val last = growthProjections.lastOrNull()?.weight ?: 0.0
            return if (first > 0) ((last - first) / first) * 100 else 0.0
        }
}

data class GrowthProjection(
    val dayOffset: Int,
    val timestamp: Long,
    val weight: Double // in kg
)
