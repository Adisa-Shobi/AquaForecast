package com.example.aquaforecast.ui.dataentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.ValidationHelper
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EntryViewModel(
    private val farmDataRepository: FarmDataRepository,
    private val pondRepository: PondRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EntryState())
    val state = _state.asStateFlow()

    init {
        loadPonds()
    }

    /**
     * Refresh ponds list - call when navigating to this screen
     */
    fun refreshPonds() {
        loadPonds()
    }

    /**
     * Load all available ponds
     */
    private fun loadPonds() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    val selectedPond = ponds.firstOrNull()
                    _state.update {
                        it.copy(
                            availablePonds = ponds,
                            selectedPond = selectedPond,
                            pondId = selectedPond?.id?.toString(),
                            pondName = selectedPond?.name,
                            isLoading = false,
                            error = if (ponds.isEmpty()) "No ponds available. Please create a pond first." else null
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load ponds: $message"
                        )
                    }
                }
        }
    }

    /**
     * Manually refresh pond list
     * Called by pull-to-refresh or user action
     */
    fun refreshPondInfo() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            pondRepository.getAllPonds()
                .onSuccess { ponds ->
                    // Keep current selection if it still exists, otherwise select first
                    val currentId = _state.value.selectedPond?.id
                    val selectedPond = ponds.firstOrNull { it.id == currentId } ?: ponds.firstOrNull()

                    _state.update {
                        it.copy(
                            availablePonds = ponds,
                            selectedPond = selectedPond,
                            pondId = selectedPond?.id?.toString(),
                            pondName = selectedPond?.name,
                            isRefreshing = false,
                            error = if (ponds.isEmpty()) "No ponds available. Please create a pond first." else null
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = "Failed to load ponds: $message"
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
                pondId = pond.id.toString(),
                pondName = pond.name,
                isPondDropdownExpanded = false
            )
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

    /**
     * Update temperature field and validate
     */
    fun onTemperatureChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getTemperatureStatus(it) }

        _state.update {
            it.copy(
                temperature = filteredValue,
                temperatureStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Update pH field and validate
     */
    fun onPhChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getPhStatus(it) }

        _state.update {
            it.copy(
                ph = filteredValue,
                phStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Update dissolved oxygen field and validate
     */
    fun onDissolvedOxygenChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getDissolvedOxygenStatus(it) }

        _state.update {
            it.copy(
                dissolvedOxygen = filteredValue,
                dissolvedOxygenStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Update ammonia field and validate
     */
    fun onAmmoniaChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getAmmoniaStatus(it) }

        _state.update {
            it.copy(
                ammonia = filteredValue,
                ammoniaStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Update nitrate field and validate
     */
    fun onNitrateChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getNitrateStatus(it) }

        _state.update {
            it.copy(
                nitrate = filteredValue,
                nitrateStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Update turbidity field and validate
     */
    fun onTurbidityChange(value: String) {
        val filteredValue = filterNumericInput(value)
        val status = filteredValue.toDoubleOrNull()?.let { ValidationHelper.getTurbidityStatus(it) }

        _state.update {
            it.copy(
                turbidity = filteredValue,
                turbidityStatus = status
            )
        }
        updateCanSave()
    }

    /**
     * Save the farm data offline (will sync when online)
     */
    fun saveData() {
        val currentState = _state.value

        // Validate pond is configured
        if (currentState.pondId == null) {
            _state.update { it.copy(error = "No pond configured") }
            return
        }

        // Validate all fields are filled
        if (!currentState.areAllFieldsFilled()) {
            _state.update { it.copy(error = "Please fill all fields") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                val farmData = FarmData(
                    temperature = currentState.temperature.toDouble(),
                    ph = currentState.ph.toDouble(),
                    dissolvedOxygen = currentState.dissolvedOxygen.toDouble(),
                    ammonia = currentState.ammonia.toDouble(),
                    nitrate = currentState.nitrate.toDouble(),
                    turbidity = currentState.turbidity.toDouble(),
                    timestamp = System.currentTimeMillis(),
                    pondId = currentState.pondId,
                    isSynced = false // Will be synced by WorkManager
                )

                farmDataRepository.save(farmData)
                    .onSuccess {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                successMessage = "Data saved successfully! Will sync when online.",
                                // Reset form
                                temperature = "",
                                ph = "",
                                dissolvedOxygen = "",
                                ammonia = "",
                                nitrate = "",
                                turbidity = "",
                                temperatureStatus = null,
                                phStatus = null,
                                dissolvedOxygenStatus = null,
                                ammoniaStatus = null,
                                nitrateStatus = null,
                                turbidityStatus = null,
                                canSave = false
                            )
                        }
                        // Clear success message after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        _state.update { it.copy(successMessage = null) }
                    }
                    .onError { message ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = "Failed to save data: $message"
                            )
                        }
                    }
            } catch (e: NumberFormatException) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Invalid number format in one or more fields"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Update canSave flag based on field validation
     */
    private fun updateCanSave() {
        val currentState = _state.value
        val canSave = currentState.areAllFieldsFilled() &&
                currentState.pondId != null

        _state.update { it.copy(canSave = canSave) }
    }

    /**
     * Show report death dialog
     */
    fun showReportDeathDialog() {
        _state.update { it.copy(showReportDeathDialog = true) }
    }

    /**
     * Hide report death dialog
     */
    fun hideReportDeathDialog() {
        _state.update { it.copy(showReportDeathDialog = false) }
    }

    /**
     * Show harvest confirmation dialog
     */
    fun showHarvestDialog() {
        _state.update { it.copy(showHarvestDialog = true) }
    }

    /**
     * Hide harvest confirmation dialog
     */
    fun hideHarvestDialog() {
        _state.update { it.copy(showHarvestDialog = false) }
    }

    /**
     * Report fish deaths and update stock count
     */
    fun reportFishDeaths(deathCount: Int) {
        viewModelScope.launch {
            val pondId = _state.value.selectedPond?.id ?: return@launch

            _state.update { it.copy(showReportDeathDialog = false) }

            pondRepository.reportFishDeaths(pondId, deathCount)
                .onSuccess {
                    _state.update {
                        it.copy(
                            successMessage = "Fish deaths reported. Stock count updated."
                        )
                    }
                    // Refresh pond info to get updated stock count
                    refreshPondInfo()
                }
                .onError { message ->
                    _state.update {
                        it.copy(error = message)
                    }
                }
        }
    }

    /**
     * Mark pond as harvested
     */
    fun markPondAsHarvested() {
        viewModelScope.launch {
            val pondId = _state.value.selectedPond?.id ?: return@launch

            _state.update { it.copy(showHarvestDialog = false) }

            pondRepository.markPondAsHarvested(pondId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            successMessage = "Pond marked as harvested. It is now read-only."
                        )
                    }
                    // Refresh pond info to get updated harvest status
                    refreshPondInfo()
                }
                .onError { message ->
                    _state.update {
                        it.copy(error = message)
                    }
                }
        }
    }

    /**
     * Filter input to allow only valid numeric values (including decimals)
     */
    private fun filterNumericInput(value: String): String {
        // Allow empty string, digits, and single decimal point
        return value.filter { it.isDigit() || it == '.' }
            .let { filtered ->
                // Ensure only one decimal point
                val parts = filtered.split(".")
                if (parts.size > 2) {
                    parts[0] + "." + parts.drop(1).joinToString("")
                } else {
                    filtered
                }
            }
    }
}
