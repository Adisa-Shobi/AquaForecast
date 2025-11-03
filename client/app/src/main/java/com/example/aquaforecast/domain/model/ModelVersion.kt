package com.example.aquaforecast.domain.model

/**
 * Minimal model information for client-side tracking
 */
data class ModelVersion(
    val version: String,
    val lastUpdated: Long
)
