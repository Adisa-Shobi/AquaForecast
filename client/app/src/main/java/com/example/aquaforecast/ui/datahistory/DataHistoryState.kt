package com.example.aquaforecast.ui.datahistory

import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond

enum class DataHistoryTab {
    LIST,
    TRENDS
}

data class DataHistoryState(
    val isLoading: Boolean = false,
    val availablePonds: List<Pond> = emptyList(),
    val selectedPond: Pond? = null,
    val farmDataList: List<FarmData> = emptyList(),
    val isPondDropdownExpanded: Boolean = false,
    val error: String? = null,
    val editingData: FarmData? = null,
    val isEditDialogOpen: Boolean = false,
    val selectedTab: DataHistoryTab = DataHistoryTab.LIST
)
