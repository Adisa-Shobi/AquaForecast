package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.ModelVersion

/**
 * Repository for managing ML model versions and updates
 */
interface ModelRepository {
    /**
     * Get the currently installed model version
     */
    suspend fun getCurrentModelVersion(): Result<ModelVersion>

    /**
     * Check if a new model version is available
     */
    suspend fun checkForModelUpdate(): Result<ModelVersion?>

    /**
     * Download and install the latest model
     * Returns the new model version if successful
     */
    suspend fun updateModel(): Result<ModelVersion>
}
