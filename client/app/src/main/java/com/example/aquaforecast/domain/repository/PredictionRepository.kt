package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.Prediction
import kotlinx.coroutines.flow.Flow

interface PredictionRepository {

    /**
     * Save a new prediction to the database
     * Called after ML model generates a new yield forecast
     *
     * @param prediction The prediction containing weight, length, and harvest date forecasts
     * @return Result.Success with the generated prediction ID if saved successfully,
     *         Result.Error with message if save failed
     */
    suspend fun save(prediction: Prediction): Result<Long>

    /**
     * Get the most recent prediction for a specific pond
     * Used to display current yield forecast on dashboard
     *
     * @param pondId The unique identifier of the pond
     * @return Result.Success with the latest prediction or null if no predictions exist,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getLatest(pondId: String): Result<Prediction?>

    /**
     * Retrieve all predictions for a specific pond
     * Used for historical analysis and trend visualization
     *
     * @param pondId The unique identifier of the pond
     * @return Result.Success with list of all predictions ordered by creation date (newest first),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getAll(pondId: String): Result<List<Prediction>>

    /**
     * Retrieve recent predictions for a specific pond
     * Used to show prediction history with limited results
     *
     * @param pondId The unique identifier of the pond
     * @param limit Maximum number of predictions to retrieve (default: 10)
     * @return Result.Success with list of recent predictions ordered by creation date (newest first),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getRecent(pondId: String, limit: Int = 10): Result<List<Prediction>>

    /**
     * Observe the latest prediction for a specific pond as a reactive stream
     * Automatically emits new values when predictions are updated
     * Used for real-time UI updates on dashboard
     *
     * @param pondId The unique identifier of the pond
     * @return Flow that emits the latest prediction whenever it changes, or null if no predictions exist
     */
    fun observe(pondId: String): Flow<Prediction?>
}