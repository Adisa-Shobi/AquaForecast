package com.example.aquaforecast.ui.dataentry

import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.WaterQualityStatus

/**
 * Location type selection for data entry
 */
enum class LocationType {
    CURRENT,
    LAST_KNOWN
}

/**
 * UI State for data entry screen
 */
data class EntryState(
    // Form fields
    val temperature: String = "",
    val ph: String = "",
    val dissolvedOxygen: String = "",
    val ammonia: String = "",
    val nitrate: String = "",
    val turbidity: String = "",

    // Visual indicators (status for each field)
    val temperatureStatus: WaterQualityStatus? = null,
    val phStatus: WaterQualityStatus? = null,
    val dissolvedOxygenStatus: WaterQualityStatus? = null,
    val ammoniaStatus: WaterQualityStatus? = null,
    val nitrateStatus: WaterQualityStatus? = null,
    val turbidityStatus: WaterQualityStatus? = null,

    // Pond info
    val pondId: String? = null,
    val pondName: String? = null,
    val availablePonds: List<Pond> = emptyList(),
    val selectedPond: Pond? = null,
    val isPondDropdownExpanded: Boolean = false,

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Dialog states
    val showReportDeathDialog: Boolean = false,
    val showHarvestDialog: Boolean = false,
    val showPredictionVerificationDialog: Boolean = false,

    // Prediction verification
    val predictedWeight: Double? = null,
    val predictedLength: Double? = null,
    val savedFarmDataId: Long? = null,

    // Location
    val locationPermissionGranted: Boolean = false,
    val shouldRequestLocationPermission: Boolean = false,
    val locationType: LocationType = LocationType.CURRENT,
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationError: String? = null,

    // Validation
    val canSave: Boolean = false
) {
    /**
     * Check if all required fields are filled with valid values
     */
    fun areAllFieldsFilled(): Boolean {
        return temperature.isNotBlank() &&
                ph.isNotBlank() &&
                dissolvedOxygen.isNotBlank() &&
                ammonia.isNotBlank() &&
                nitrate.isNotBlank() &&
                turbidity.isNotBlank()
    }

    /**
     * Check if any field has critical status
     */
    fun hasCriticalValues(): Boolean {
        return temperatureStatus == WaterQualityStatus.CRITICAL ||
                phStatus == WaterQualityStatus.CRITICAL ||
                dissolvedOxygenStatus == WaterQualityStatus.CRITICAL ||
                ammoniaStatus == WaterQualityStatus.CRITICAL ||
                nitrateStatus == WaterQualityStatus.CRITICAL ||
                turbidityStatus == WaterQualityStatus.CRITICAL
    }
}
