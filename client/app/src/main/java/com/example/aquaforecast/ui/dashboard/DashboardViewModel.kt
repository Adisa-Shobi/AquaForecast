package com.example.aquaforecast.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.data.ml.MLPredictor
import com.example.aquaforecast.data.preferences.PreferencesManager
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DashboardViewModel(
    private val pondRepository: PondRepository,
    private val predictionRepository: PredictionRepository,
    private val farmDataRepository: FarmDataRepository,
    private val mlPredictor: MLPredictor,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        // Load forecast horizon preference
        viewModelScope.launch {
            preferencesManager.forecastHorizon.collect { days ->
                _state.update { it.copy(forecastHorizonDays = days) }
            }
        }
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load all ponds
            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    // Keep current selection if it still exists, otherwise select first
                    val currentId = _state.value.selectedPond?.id
                    val selectedPond = ponds.firstOrNull { it.id == currentId } ?: ponds.firstOrNull()

                    _state.update {
                        it.copy(
                            availablePonds = ponds,
                            selectedPond = selectedPond,
                            pond = selectedPond
                        )
                    }

                    // If pond exists, load its data
                    selectedPond?.let { p ->
                        loadPondData(p.id.toString())
                    } ?: run {
                        // No ponds available
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = message
                        )
                    }
                }
        }
    }

    /**
     * Handle pond selection from dropdown
     */
    fun onPondSelected(pond: Pond) {
        _state.update {
            it.copy(
                selectedPond = pond,
                pond = pond,
                isPondDropdownExpanded = false,
                isLoading = true,
                // Clear previous pond's data and error state
                latestPrediction = null,
                latestFarmData = null,
                growthProjections = emptyList(),
                error = null
            )
        }
        // Load data for the newly selected pond
        viewModelScope.launch {
            loadPondData(pond.id.toString())
        }
    }

    /**
     * Toggle pond dropdown visibility
     */
    fun togglePondDropdown() {
        _state.update {
            it.copy(isPondDropdownExpanded = !it.isPondDropdownExpanded)
        }
    }

    /**
     * Dismiss pond dropdown
     */
    fun dismissPondDropdown() {
        _state.update {
            it.copy(isPondDropdownExpanded = false)
        }
    }

    private suspend fun loadPondData(pondId: String) {
        // Load latest prediction
        predictionRepository.getLatest(pondId)
            .onSuccess { prediction ->
                _state.update { it.copy(latestPrediction = prediction) }
            }
            .onError { message ->
                // Log error but don't block the UI
                println("Error loading prediction: $message")
                _state.update { it.copy(latestPrediction = null) }
            }

        // Load latest farm data
        farmDataRepository.getLatest(pondId)
            .onSuccess { farmData ->
                _state.update {
                    it.copy(
                        latestFarmData = farmData,
                        isLoading = false
                    )
                }
            }
            .onError { message ->
                _state.update {
                    it.copy(
                        latestFarmData = null,
                        isLoading = false,
                        error = message
                    )
                }
            }

        // Generate growth projections (will clear if no data)
        generateGrowthProjections()
    }

    private suspend fun generateGrowthProjections() {
        val pond = _state.value.pond ?: return
        val currentPrediction = _state.value.latestPrediction
        val horizonDays = _state.value.forecastHorizonDays

        // If no prediction exists, clear projections and try to generate one
        if (currentPrediction == null) {
            _state.update { it.copy(growthProjections = emptyList()) }
            generateNewPrediction()
            return
        }

        // Generate daily growth projections
        val projections = mutableListOf<GrowthProjection>()
        val currentTime = System.currentTimeMillis()
        val currentWeight = currentPrediction.predictedWeight
        val targetWeight = 1.0 // 1kg target for catfish
        val growthRatePerDay = (targetWeight - currentWeight) / horizonDays

        for (day in 0 until horizonDays) {
            val weight = currentWeight + (growthRatePerDay * day)
            projections.add(
                GrowthProjection(
                    dayOffset = day,
                    timestamp = currentTime + TimeUnit.DAYS.toMillis(day.toLong()),
                    weight = weight.coerceAtLeast(currentWeight)
                )
            )
        }

        _state.update { it.copy(growthProjections = projections) }
    }

    fun generateNewPrediction() {
        viewModelScope.launch {
            val pond = _state.value.pond ?: return@launch

            _state.update { it.copy(isRefreshing = true) }

            mlPredictor.predict(pond)
                .onSuccess { prediction ->
                    // Save prediction to database
                    predictionRepository.save(prediction)
                        .onSuccess {
                            _state.update { it.copy(latestPrediction = prediction) }
                            generateGrowthProjections()
                        }
                }
                .onError { message ->
                    _state.update { it.copy(error = message) }
                }

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            // Generate new prediction and reload data
            generateNewPrediction()

            val pondId = _state.value.pond?.id?.toString()
            if (pondId != null) {
                loadPondData(pondId)
            }

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun showReportDeathDialog() {
        _state.update { it.copy(showReportDeathDialog = true) }
    }

    fun hideReportDeathDialog() {
        _state.update { it.copy(showReportDeathDialog = false) }
    }

    fun showHarvestDialog() {
        _state.update { it.copy(showHarvestDialog = true) }
    }

    fun hideHarvestDialog() {
        _state.update { it.copy(showHarvestDialog = false) }
    }

    fun reportFishDeaths(deathCount: Int) {
        viewModelScope.launch {
            val pondId = _state.value.pond?.id ?: return@launch

            pondRepository.reportFishDeaths(pondId, deathCount)
                .onSuccess {
                    _state.update {
                        it.copy(
                            successMessage = "Fish deaths reported. Stock count updated.",
                            showReportDeathDialog = false
                        )
                    }
                    // Reload pond data
                    loadPondData(pondId.toString())
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            error = message,
                            showReportDeathDialog = false
                        )
                    }
                }
        }
    }

    fun markPondAsHarvested() {
        viewModelScope.launch {
            val pondId = _state.value.pond?.id ?: return@launch

            pondRepository.markPondAsHarvested(pondId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            successMessage = "Pond marked as harvested. It is now read-only.",
                            showHarvestDialog = false
                        )
                    }
                    // Reload pond data
                    loadPondData(pondId.toString())
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            error = message,
                            showHarvestDialog = false
                        )
                    }
                }
        }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }
}
