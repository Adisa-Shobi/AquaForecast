package com.example.aquaforecast.domain.model

import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class Prediction(
    val id: Long = 0,
    val predictedWeight: Double,
    val predictedLength: Double,
    val harvestDate: Long,
    val confidence: Double = 0.0,  // Model confidence score (0.0 - 1.0)
    val createdAt: Long = System.currentTimeMillis(),
    val pondId: String
) {
    val daysUntilHarvest: Int
        get() {
            val diff = harvestDate - System.currentTimeMillis()
            return TimeUnit.MILLISECONDS.toDays(diff).toInt()
        }

    val weeksUntilHarvest: Int
        get() = (daysUntilHarvest / 7).coerceAtLeast(0)

    val isHarvestReady: Boolean
        get() = harvestDate <= System.currentTimeMillis()

    val isHarvestApproaching: Boolean
        get() = daysUntilHarvest in 1..14

    fun getFormattedWeight(): String {
        return String.format("%.2f kg", predictedWeight)
    }

    fun getFormattedLength(): String {
        return String.format("%.1f cm", predictedLength)
    }

    fun getFormattedHarvestDate(): String {
        val date = java.util.Date(harvestDate)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }


    fun getConfidencePercentage(): String {
        return String.format("%.0f%%", confidence * 100)
    }

    fun getHarvestStatusText(): String {
        return when {
            isHarvestReady -> "Ready to harvest"
            isHarvestApproaching -> "Approaching harvest ($daysUntilHarvest days)"
            weeksUntilHarvest > 0 -> "Harvest in $weeksUntilHarvest weeks"
            else -> "Harvest in $daysUntilHarvest days"
        }
    }

    fun calculateTotalYield(stockCount: Int): Double {
        return predictedWeight * stockCount
    }
}