package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.FarmDataDao
import com.example.aquaforecast.data.local.entity.FarmDataEntity
import com.example.aquaforecast.domain.model.FarmData
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError

/**
 * Implementation of FarmDataRepository
 * Handles farm data operations with error handling and domain/entity mapping
 */
class FarmDataRepositoryImpl(
    private val farmDataDao: FarmDataDao
) : FarmDataRepository {

    override suspend fun save(farmData: FarmData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            farmDataDao.insert(farmData.toEntity())
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to save farm data").asError()
        }
    }

    override suspend fun getAll(): Result<List<FarmData>> = withContext(Dispatchers.IO) {
        try {
            val entities = farmDataDao.getAll()
            entities.map { it.toDomain() }.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve farm data").asError()
        }
    }

    override suspend fun getByDateRange(startDate: Long, endDate: Long): Result<List<FarmData>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = farmDataDao.getByDateRange(startDate, endDate)
                entities.map { it.toDomain() }.asSuccess()
            } catch (e: Exception) {
                (e.message ?: "Failed to retrieve farm data by date range").asError()
            }
        }

    override suspend fun getLatest(pondId: String): Result<FarmData?> = withContext(Dispatchers.IO) {
        try {
            val entity = farmDataDao.getLatestByPond(pondId)
            entity?.toDomain().asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve latest farm data").asError()
        }
    }

    override suspend fun getRecent(pondId: String, limit: Int): Result<List<FarmData>> =
        withContext(Dispatchers.IO) {
            try {
                val entities = farmDataDao.getRecentByPond(pondId, limit)
                entities.map { it.toDomain() }.asSuccess()
            } catch (e: Exception) {
                (e.message ?: "Failed to retrieve recent farm data").asError()
            }
        }

    override suspend fun getUnsynced(): Result<List<FarmData>> = withContext(Dispatchers.IO) {
        try {
            val entities = farmDataDao.getUnsyncedData()
            entities.map { it.toDomain() }.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve unsynced data").asError()
        }
    }

    override suspend fun markSynced(ids: List<Long>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            farmDataDao.markAsSynced(ids)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to mark data as synced").asError()
        }
    }
}

// Mapper Extensions
/**
 * Convert FarmData domain model to FarmDataEntity for database storage
 */
private fun FarmData.toEntity(): FarmDataEntity {
    return FarmDataEntity(
        id = id,
        temperature = temperature,
        ph = ph,
        dissolvedOxygen = dissolvedOxygen,
        ammonia = ammonia,
        nitrate = nitrate,
        turbidity = turbidity,
        timestamp = timestamp,
        pondId = pondId,
        isSynced = isSynced
    )
}

/**
 * Convert FarmDataEntity from database to FarmData domain model
 */
private fun FarmDataEntity.toDomain(): FarmData {
    return FarmData(
        id = id,
        temperature = temperature,
        ph = ph,
        dissolvedOxygen = dissolvedOxygen,
        ammonia = ammonia,
        nitrate = nitrate,
        turbidity = turbidity,
        timestamp = timestamp,
        pondId = pondId,
        isSynced = isSynced
    )
}