// data/repository/PondRepositoryImpl.kt
package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.PondDao
import com.example.aquaforecast.data.local.entity.PondEntity
import com.example.aquaforecast.domain.model.Pond
import com.example.aquaforecast.domain.model.Species
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    override suspend fun savePondConfig(
        pondName: String,
        species: String,
        initialStockCount: Int,
        startDate: LocalDate
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val speciesEnum = when (species) {
                "Tilapia" -> Species.TILAPIA
                "Catfish" -> Species.CATFISH
                else -> throw IllegalArgumentException("Invalid species: $species")
            }

            val existingPond = pondDao.getPond()

            if (existingPond != null) {
                val updatedPond = Pond(
                    id = existingPond.id,
                    name = pondName,
                    species = speciesEnum,
                    stockCount = initialStockCount,
                    startDate = startDate,
                    createdAt = existingPond.createdAt
                )
                pondDao.update(updatedPond.toEntity())
            } else {
                val newPond = Pond(
                    id = 0,
                    name = pondName,
                    species = speciesEnum,
                    stockCount = initialStockCount,
                    startDate = startDate,
                    createdAt = System.currentTimeMillis()
                )
                pondDao.insert(newPond.toEntity())
            }

            Unit.asSuccess()
        } catch (e: IllegalArgumentException) {
            (e.message ?: "Invalid pond configuration").asError()
        } catch (e: Exception) {
            (e.message ?: "Failed to save pond configuration").asError()
        }
    }

    override suspend fun getPondConfig(): Result<Pond?> = withContext(Dispatchers.IO) {
        try {
            val entity = pondDao.getPond()
            entity?.toDomain().asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to get pond configuration").asError()
        }
    }

    override fun observePondConfig(): Flow<Pond?> {
        return pondDao.observePond().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun deletePondConfig(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pondDao.deleteAll()
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to delete pond configuration").asError()
        }
    }
}

private fun Pond.toEntity(): PondEntity {
    return PondEntity(
        id = id,
        name = name,
        species = species.name,
        stockCount = stockCount,
        startDate = startDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        createdAt = createdAt
    )
}

private fun PondEntity.toDomain(): Pond {
    return Pond(
        id = id,
        name = name,
        species = Species.valueOf(species),
        stockCount = stockCount,
        startDate = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
        createdAt = createdAt
    )
}