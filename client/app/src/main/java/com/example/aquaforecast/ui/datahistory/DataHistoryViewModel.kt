package com.example.aquaforecast.ui.datahistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DataHistoryViewModel(
    private val farmDataRepository: FarmDataRepository,
    private val pondRepository: PondRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DataHistoryState())
    val state = _state.asStateFlow()

    init {
        loadPonds()
    }

    fun loadPonds() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    _state.update {
                        it.copy(
                            availablePonds = ponds,
                            selectedPond = ponds.firstOrNull(),
                            isLoading = false
                        )
                    }
                    // Load data for first pond
                    ponds.firstOrNull()?.let { pond ->
                        loadFarmData(pond.id.toString())
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            error = message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onPondSelected(pond: Pond) {
        _state.update {
            it.copy(
                selectedPond = pond,
                isPondDropdownExpanded = false,
                farmDataList = emptyList()
            )
        }
        loadFarmData(pond.id.toString())
    }

    fun togglePondDropdown() {
        _state.update {
            it.copy(isPondDropdownExpanded = !it.isPondDropdownExpanded)
        }
    }

    private fun loadFarmData(pondId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            farmDataRepository.getRecent(pondId, limit = 100)
                .onSuccess { dataList ->
                    _state.update {
                        it.copy(
                            farmDataList = dataList,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            error = message,
                            isLoading = false,
                            farmDataList = emptyList()
                        )
                    }
                }
        }
    }

    fun openEditDialog(farmData: FarmData) {
        _state.update {
            it.copy(
                editingData = farmData,
                isEditDialogOpen = true
            )
        }
    }

    fun closeEditDialog() {
        _state.update {
            it.copy(
                editingData = null,
                isEditDialogOpen = false
            )
        }
    }

    fun updateFarmData(farmData: FarmData) {
        viewModelScope.launch {
            farmDataRepository.save(farmData)
                .onSuccess {
                    closeEditDialog()
                    // Reload data for current pond
                    _state.value.selectedPond?.let { pond ->
                        loadFarmData(pond.id.toString())
                    }
                }
                .onError { message ->
                    _state.update { it.copy(error = message) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun selectTab(tab: DataHistoryTab) {
        _state.update { it.copy(selectedTab = tab) }
    }
}
