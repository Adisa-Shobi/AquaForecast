package com.example.aquaforecast.ui.dataentry

import com.example.aquaforecast.domain.model.WaterQualityStatus

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

    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

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
