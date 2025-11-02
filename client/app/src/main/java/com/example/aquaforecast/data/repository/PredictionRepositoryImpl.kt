package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.PredictionDao
import com.example.aquaforecast.data.local.entity.PredictionEntity
import com.example.aquaforecast.domain.model.Prediction
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of PredictionRepository
 * Handles prediction operations with error handling and reactive data streams
 */
class PredictionRepositoryImpl(
    private val predictionDao: PredictionDao
) : PredictionRepository {

    override suspend fun save(prediction: Prediction): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = predictionDao.insert(prediction.toEntity())
            id.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to save prediction").asError()
        }
    }

    override suspend fun getLatest(pondId: String): Result<Prediction?> = withContext(Dispatchers.IO) {
        try {
            val entity = predictionDao.getLatest(pondId)
            entity?.toDomain().asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve latest prediction").asError()
        }
    }

    override suspend fun getAll(pondId: String): Result<List<Prediction>> = withContext(Dispatchers.IO) {
        try {
            val entities = predictionDao.getAllByPond(pondId)
            entities.map { it.toDomain() }.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve predictions").asError()
        }
    }

    override suspend fun getRecent(pondId: String, limit: Int): Result<List<Prediction>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = predictionDao.getRecentByPond(pondId, limit)
                entities.map { it.toDomain() }.asSuccess()
            } catch (e: Exception) {
                (e.message ?: "Failed to retrieve recent predictions").asError()
            }
        }

    override fun observe(pondId: String): Flow<Prediction?> {
        return predictionDao.observeLatest(pondId)
            .map { entity -> entity?.toDomain() }
    }
}

// Mapper Extensions
/**
 * Convert Prediction domain model to PredictionEntity for database storage
 */
private fun Prediction.toEntity(): PredictionEntity {
    return PredictionEntity(
        id = id,
        predictedWeight = predictedWeight,
        predictedLength = predictedLength,
        harvestDate = harvestDate,
        createdAt = createdAt,
        pondId = pondId
    )
}

/**
 * Convert PredictionEntity from database to Prediction domain model
 */
private fun PredictionEntity.toDomain(): Prediction {
    return Prediction(
        id = id,
        predictedWeight = predictedWeight,
        predictedLength = predictedLength,
        harvestDate = harvestDate,
        confidence = 0.0,  // Default confidence, can be added to entity later
        createdAt = createdAt,
        pondId = pondId
    )
}