package com.example.aquaforecast.ui.dataentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquaforecast.data.ml.MLPredictor
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Prediction
import com.example.aquaforecast.domain.model.ValidationHelper
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.onError
import com.example.aquaforecast.domain.repository.onSuccess
import com.example.aquaforecast.domain.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EntryViewModel(
    private val farmDataRepository: FarmDataRepository,
    private val pondRepository: PondRepository,
    private val predictionRepository: PredictionRepository,
    private val mlPredictor: MLPredictor,
    private val locationService: LocationService
) : ViewModel() {

    private val _state = MutableStateFlow(EntryState())
    val state = _state.asStateFlow()

    init {
        loadPonds()
        checkLocationPermission()
    }

    /**
     * Check if location permission is granted
     */
    private fun checkLocationPermission() {
        val hasPermission = locationService.hasLocationPermission()
        _state.update { it.copy(locationPermissionGranted = hasPermission) }
    }

    /**
     * Request location permission
     */
    fun requestLocationPermission() {
        _state.update { it.copy(shouldRequestLocationPermission = true) }
    }

    /**
     * Handle location permission result
     */
    fun onLocationPermissionResult(granted: Boolean) {
        _state.update {
            it.copy(
                locationPermissionGranted = granted,
                shouldRequestLocationPermission = false,
                locationError = if (!granted) "Location permission denied. Data will be saved without location." else null
            )
        }

        // If granted, capture location immediately
        if (granted) {
            captureLocation()
        }
    }

    /**
     * Change location type (Current vs Last Known)
     */
    fun onLocationTypeChange(locationType: LocationType) {
        _state.update { it.copy(locationType = locationType, capturedLatitude = null, capturedLongitude = null) }

        // Auto-capture for last known location
        if (locationType == LocationType.LAST_KNOWN && locationService.hasLocationPermission()) {
            captureLocation()
        }
    }

    /**
     * Capture location based on selected type
     */
    fun captureLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isCapturingLocation = true, locationError = null) }

            // Check permission first
            if (!locationService.hasLocationPermission()) {
                _state.update {
                    it.copy(
                        isCapturingLocation = false,
                        locationError = "Location permission required",
                        shouldRequestLocationPermission = true
                    )
                }
                return@launch
            }

            val currentState = _state.value
            val locationResult = when (currentState.locationType) {
                LocationType.CURRENT -> locationService.getCurrentLocation()
                LocationType.LAST_KNOWN -> locationService.getLastUsedLocation()
            }

            locationResult
                .onSuccess { location ->
                    if (location != null) {
                        _state.update {
                            it.copy(
                                capturedLatitude = location.latitude,
                                capturedLongitude = location.longitude,
                                isCapturingLocation = false,
                                locationError = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isCapturingLocation = false,
                                locationError = when (currentState.locationType) {
                                    LocationType.CURRENT -> "Unable to get current location. Please try again or use last used location."
                                    LocationType.LAST_KNOWN -> "No previously used location found. Please capture current location first."
                                }
                            )
                        }
                    }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            isCapturingLocation = false,
                            locationError = message
                        )
                    }
                }
        }
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
                // Use captured location from state (already captured via UI button)
                val farmData = FarmData(
                    temperature = currentState.temperature.toDouble(),
                    ph = currentState.ph.toDouble(),
                    dissolvedOxygen = currentState.dissolvedOxygen.toDouble(),
                    ammonia = currentState.ammonia.toDouble(),
                    nitrate = currentState.nitrate.toDouble(),
                    turbidity = currentState.turbidity.toDouble(),
                    timestamp = System.currentTimeMillis(),
                    pondId = currentState.pondId,
                    latitude = currentState.capturedLatitude,
                    longitude = currentState.capturedLongitude,
                    isSynced = false // Will be synced by WorkManager
                )

                farmDataRepository.save(farmData)
                    .onSuccess { farmDataId ->
                        // Save location to preferences for future "Last Known Location" use
                        if (currentState.capturedLatitude != null && currentState.capturedLongitude != null) {
                            locationService.saveLastUsedLocation(
                                currentState.capturedLatitude,
                                currentState.capturedLongitude
                            )
                        }

                        // Generate prediction after saving farm data
                        val pond = currentState.selectedPond
                        if (pond != null) {
                            generatePrediction(pond, farmDataId)
                        } else {
                            // If no pond selected, just show success
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
                        }
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
     * Generate prediction using ML model and show verification dialog
     */
    private fun generatePrediction(pond: Pond, farmDataId: Long) {
        viewModelScope.launch {
            mlPredictor.predict(pond)
                .onSuccess { prediction ->
                    // Show verification dialog with predicted values
                    _state.update {
                        it.copy(
                            isSaving = false,
                            predictedWeight = prediction.predictedWeight,
                            predictedLength = prediction.predictedLength,
                            savedFarmDataId = farmDataId,
                            showPredictionVerificationDialog = true
                        )
                    }
                }
                .onError { message ->
                    // If prediction fails, still show success for data entry
                    _state.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Data saved! Prediction unavailable: $message",
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
                }
        }
    }

    /**
     * Hide prediction verification dialog
     */
    fun hidePredictionVerificationDialog() {
        _state.update {
            it.copy(showPredictionVerificationDialog = false)
        }
    }

    /**
     * Verify prediction - user confirms values are accurate
     */
    fun verifyPrediction(isAccurate: Boolean) {
        viewModelScope.launch {
            val currentState = _state.value
            val farmDataId = currentState.savedFarmDataId ?: return@launch
            val pond = currentState.selectedPond ?: return@launch

            // Create prediction with verification status
            val prediction = Prediction(
                predictedWeight = currentState.predictedWeight ?: 0.0,
                predictedLength = currentState.predictedLength ?: 0.0,
                harvestDate = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(30),
                createdAt = System.currentTimeMillis(),
                pondId = pond.id.toString(),
                farmDataId = farmDataId,
                verified = isAccurate
            )

            predictionRepository.save(prediction)
                .onSuccess {
                    _state.update {
                        it.copy(
                            showPredictionVerificationDialog = false,
                            successMessage = if (isAccurate) {
                                "Data and prediction saved successfully!"
                            } else {
                                "Data saved. Prediction marked as inaccurate."
                            },
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
                            canSave = false,
                            predictedWeight = null,
                            predictedLength = null,
                            savedFarmDataId = null
                        )
                    }
                    // Clear success message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    _state.update { it.copy(successMessage = null) }
                }
                .onError { message ->
                    _state.update {
                        it.copy(
                            showPredictionVerificationDialog = false,
                            error = "Failed to save prediction: $message"
                        )
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
