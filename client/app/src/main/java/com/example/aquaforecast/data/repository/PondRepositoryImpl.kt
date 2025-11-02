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

    override suspend fun getAllPonds(): Result<List<Pond>> = withContext(Dispatchers.IO) {
        try {
            val entities = pondDao.getAllPonds()
            entities.map { it.toDomain() }.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to get all ponds").asError()
        }
    }

    override suspend fun getPondById(pondId: Long): Result<Pond?> = withContext(Dispatchers.IO) {
        try {
            val entity = pondDao.getPondById(pondId)
            entity?.toDomain().asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to get pond by ID").asError()
        }
    }

    override suspend fun deletePondById(pondId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pondDao.deleteById(pondId)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to delete pond").asError()
        }
    }

    override suspend fun getPondCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = pondDao.getCount()
            count.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to get pond count").asError()
        }
    }

    override suspend fun reportFishDeaths(pondId: Long, deathCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pond = pondDao.getPondById(pondId)
                ?: return@withContext "Pond not found".asError()

            val newStockCount = (pond.stockCount - deathCount).coerceAtLeast(0)
            val updatedPond = pond.copy(stockCount = newStockCount)
            pondDao.update(updatedPond)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to report fish deaths").asError()
        }
    }

    override suspend fun markPondAsHarvested(pondId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pond = pondDao.getPondById(pondId)
                ?: return@withContext "Pond not found".asError()

            val updatedPond = pond.copy(isHarvested = true)
            pondDao.update(updatedPond)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to mark pond as harvested").asError()
        }
    }

    override suspend fun markPondAsNotHarvested(pondId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pond = pondDao.getPondById(pondId)
                ?: return@withContext "Pond not found".asError()

            val updatedPond = pond.copy(isHarvested = false)
            pondDao.update(updatedPond)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to mark pond as not harvested").asError()
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
        createdAt = createdAt,
        isHarvested = isHarvested
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
        createdAt = createdAt,
        isHarvested = isHarvested
    )
}