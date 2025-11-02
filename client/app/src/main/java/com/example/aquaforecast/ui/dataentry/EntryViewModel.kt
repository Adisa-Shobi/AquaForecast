package com.example.aquaforecast.ui.dataentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.domain.model.FarmData
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
        observePondConfig()
    }

    /**
     * Observe pond configuration changes in real-time
     * Automatically updates when user modifies pond settings
     */
    private fun observePondConfig() {
        viewModelScope.launch {
            pondRepository.observePondConfig().collect { pond ->
                _state.update {
                    it.copy(
                        pondId = pond?.id?.toString(),
                        pondName = pond?.name,
                        isLoading = false,
                        error = if (pond == null) "No pond configured. Please configure a pond first." else null
                    )
                }
            }
        }
    }

    /**
     * Manually refresh pond configuration
     * Called by pull-to-refresh or user action
     */
    fun refreshPondInfo() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            pondRepository.get()
                .onSuccess { pond ->
                    _state.update {
                        it.copy(
                            pondId = pond?.id?.toString(),
                            pondName = pond?.name,
                            isRefreshing = false,
                            error = if (pond == null) "No pond configured. Please configure a pond first." else null
                        )
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = "Failed to load pond info: $message"
                        )
                    }
                }
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
