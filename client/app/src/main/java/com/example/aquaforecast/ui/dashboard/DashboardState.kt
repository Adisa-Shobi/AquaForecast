package com.example.aquaforecast.ui.dashboard

import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Prediction

data class DashboardState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pond: Pond? = null,
    val latestPrediction: Prediction? = null,
    val latestFarmData: FarmData? = null,
    val isRefreshing: Boolean = false,
    val isOfflineMode: Boolean = false
) {
    val hasData: Boolean
        get() = pond != null && latestPrediction != null

    val showEmptyState: Boolean
        get() = !isLoading && !hasData && error == null
}
