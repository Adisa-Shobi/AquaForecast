package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.PondDao
import com.example.aquaforecast.data.local.entity.PondEntity
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Species
import com.example.aquaforecast.domain.repository.PondRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess

class PondRepositoryImpl(
    private val pondDao: PondDao
) : PondRepository {

    override suspend fun save(pond: Pond): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = pondDao.insert(pond.toEntity())
            id.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to save pond configuration").asError()
        }
    }

    override suspend fun get(): Result<Pond?> = withContext(Dispatchers.IO) {
        try {
            val entity = pondDao.getPond()
            entity?.toDomain().asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve pond configuration").asError()
        }
    }

    override suspend fun update(pond: Pond): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pondDao.update(pond.toEntity())
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to update pond configuration").asError()
        }
    }

    override suspend fun delete(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pondDao.deleteAll()
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to delete pond configuration").asError()
        }
    }

    override suspend fun isConfigured(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val pond = pondDao.getPond()
            (pond != null).asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to check pond configuration").asError()
        }
    }
}

// Mapper Extensions
/**
 * Convert Pond domain model to PondEntity for database storage
 */
private fun Pond.toEntity(): PondEntity {
    return PondEntity(
        id = id,
        name = name,
        species = species.name,  // Store enum as string
        stockCount = stockCount,
        startDate = startDate,
        createdAt = createdAt
    )
}

/**
 * Convert PondEntity from database to Pond domain model
 */
private fun PondEntity.toDomain(): Pond {
    return Pond(
        id = id,
        name = name,
        species = Species.valueOf(species),  // Convert string to enum
        stockCount = stockCount,
        startDate = startDate,
        createdAt = createdAt
    )
}