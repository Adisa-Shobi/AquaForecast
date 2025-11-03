package com.example.aquaforecast.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FarmData(
    val id: Long = 0,
    val temperature: Double,
    val ph: Double,
    val dissolvedOxygen: Double,
    val ammonia: Double,
    val nitrate: Double,
    val turbidity: Double,
    val timestamp: Long,
    val pondId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSynced: Boolean = false
) {
    /**
     * Get overall water quality status based on all parameters
     */
    fun getOverallStatus(): WaterQualityStatus {
        val statuses = listOf(
            ValidationHelper.getTemperatureStatus(temperature),
            ValidationHelper.getPhStatus(ph),
            ValidationHelper.getDissolvedOxygenStatus(dissolvedOxygen),
            ValidationHelper.getAmmoniaStatus(ammonia),
            ValidationHelper.getNitrateStatus(nitrate),
            ValidationHelper.getTurbidityStatus(turbidity)
        )

        return when {
            statuses.any { it == WaterQualityStatus.CRITICAL } -> WaterQualityStatus.CRITICAL
            statuses.any { it == WaterQualityStatus.WARNING } -> WaterQualityStatus.WARNING
            else -> WaterQualityStatus.OPTIMAL
        }
    }

    /**
     * Get formatted timestamp
     */
    fun getFormattedDate(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * Check if data is from today
     */
    fun isToday(): Boolean {
        val today = java.util.Calendar.getInstance()
        val dataDate = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return today.get(java.util.Calendar.YEAR) == dataDate.get(java.util.Calendar.YEAR) &&
                today.get(java.util.Calendar.DAY_OF_YEAR) == dataDate.get(java.util.Calendar.DAY_OF_YEAR)
    }
}