package com.example.aquaforecast.data.repository

import com.example.aquaforecast.data.local.dao.FeedingScheduleDao
import com.example.aquaforecast.data.local.entity.FeedingScheduleEntity
import com.example.aquaforecast.domain.model.FeedingSchedule
import com.example.aquaforecast.domain.repository.FeedingScheduleRepository
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FeedingScheduleRepositoryImpl(
    private val feedingScheduleDao: FeedingScheduleDao
) : FeedingScheduleRepository {

    override suspend fun getAllSchedules(): Result<List<FeedingSchedule>> = withContext(Dispatchers.IO) {
        try {
            val schedules = feedingScheduleDao.getAllSchedules().map { it.toDomain() }
            schedules.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve feeding schedules").asError()
        }
    }

    override fun observeAllSchedules(): Flow<List<FeedingSchedule>> {
        return feedingScheduleDao.observeAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getActiveSchedules(): Result<List<FeedingSchedule>> = withContext(Dispatchers.IO) {
        try {
            val schedules = feedingScheduleDao.getActiveSchedules().map { it.toDomain() }
            schedules.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve active schedules").asError()
        }
    }

    override fun observeActiveSchedules(): Flow<List<FeedingSchedule>> {
        return feedingScheduleDao.observeActiveSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSchedulesByPond(pondId: Long): Result<List<FeedingSchedule>> = withContext(Dispatchers.IO) {
        try {
            val schedules = feedingScheduleDao.getSchedulesByPond(pondId).map { it.toDomain() }
            schedules.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve pond schedules").asError()
        }
    }

    override suspend fun getScheduleById(scheduleId: Long): Result<FeedingSchedule?> = withContext(Dispatchers.IO) {
        try {
            val schedule = feedingScheduleDao.getScheduleById(scheduleId)?.toDomain()
            schedule.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve schedule").asError()
        }
    }

    override suspend fun createSchedule(
        name: String,
        pondId: Long,
        pondName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        feedingTime: LocalTime
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val entity = FeedingScheduleEntity(
                name = name,
                pondId = pondId,
                pondName = pondName,
                startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                feedingTime = feedingTime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            val id = feedingScheduleDao.insert(entity)
            id.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to create feeding schedule").asError()
        }
    }

    override suspend fun updateSchedule(schedule: FeedingSchedule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            feedingScheduleDao.update(schedule.toEntity())
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to update feeding schedule").asError()
        }
    }

    override suspend fun deleteSchedule(scheduleId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            feedingScheduleDao.deleteById(scheduleId)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to delete feeding schedule").asError()
        }
    }

    override suspend fun toggleScheduleActive(scheduleId: Long, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            feedingScheduleDao.updateActiveStatus(scheduleId, isActive)
            Unit.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to toggle schedule status").asError()
        }
    }

    override suspend fun getCurrentSchedules(): Result<List<FeedingSchedule>> = withContext(Dispatchers.IO) {
        try {
            val currentDate = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val schedules = feedingScheduleDao.getCurrentSchedules(currentDate).map { it.toDomain() }
            schedules.asSuccess()
        } catch (e: Exception) {
            (e.message ?: "Failed to retrieve current schedules").asError()
        }
    }

    override fun observeCurrentSchedules(): Flow<List<FeedingSchedule>> {
        val currentDate = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return feedingScheduleDao.observeCurrentSchedules(currentDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

private fun FeedingScheduleEntity.toDomain(): FeedingSchedule {
    return FeedingSchedule(
        id = id,
        name = name,
        pondId = pondId,
        pondName = pondName,
        startDate = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
        endDate = Instant.ofEpochMilli(endDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
        feedingTime = LocalTime.parse(feedingTime, DateTimeFormatter.ISO_LOCAL_TIME),
        isActive = isActive,
        createdAt = createdAt
    )
}

private fun FeedingSchedule.toEntity(): FeedingScheduleEntity {
    return FeedingScheduleEntity(
        id = id,
        name = name,
        pondId = pondId,
        pondName = pondName,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        feedingTime = feedingTime.format(DateTimeFormatter.ISO_LOCAL_TIME),
        isActive = isActive,
        createdAt = createdAt
    )
}
