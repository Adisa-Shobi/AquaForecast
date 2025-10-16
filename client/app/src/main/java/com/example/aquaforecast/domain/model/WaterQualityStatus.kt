package com.example.aquaforecast.domain.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class WaterQualityStatus(
    val displayName: String,
    val description: String,
    val colorValue: Long
) {
    OPTIMAL(
        displayName = "Optimal",
        description = "All parameters are within ideal range",
        colorValue = 0xFF10B981  // Green
    ),
    WARNING(
        displayName = "Warning",
        description = "Some parameters need attention",
        colorValue = 0xFFF59E0B  // Amber
    ),
    CRITICAL(
        displayName = "Critical",
        description = "Immediate action required",
        colorValue = 0xFFEF4444  // Red
    );

    /**
     * Get color for Compose UI
     */
    fun getColor(): Color = Color(colorValue)

    /**
     * Check if action is needed
     */
    val requiresAction: Boolean
        get() = this != OPTIMAL

    /**
     * Check if urgent
     */
    val isUrgent: Boolean
        get() = this == CRITICAL
}