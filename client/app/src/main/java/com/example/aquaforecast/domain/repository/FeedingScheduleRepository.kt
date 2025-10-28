package com.example.aquaforecast.domain.repository

import com.example.aquaforecast.domain.model.FeedingSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Repository interface for feeding schedule management
 * Provides operations for creating, reading, updating, and deleting feeding schedules
 */
interface FeedingScheduleRepository {

    /**
     * Get all feeding schedules
     * Returns all schedules ordered by start date descending
     *
     * @return Result.Success with list of schedules (empty if none exist),
     *         Result.Error with message if retrieval failed
     */
    suspend fun getAllSchedules(): Result<List<FeedingSchedule>>

    /**
     * Observe all feeding schedules
     * Emits updates whenever schedules change
     *
     * @return Flow that emits list of schedules
     */
    fun observeAllSchedules(): Flow<List<FeedingSchedule>>

    /**
     * Get active schedules
     * Returns only schedules where isActive is true
     *
     * @return Result.Success with list of active schedules,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getActiveSchedules(): Result<List<FeedingSchedule>>

    /**
     * Observe active schedules
     * Emits updates whenever active schedules change
     *
     * @return Flow that emits list of active schedules
     */
    fun observeActiveSchedules(): Flow<List<FeedingSchedule>>

    /**
     * Get schedules for a specific pond
     *
     * @param pondId The ID of the pond
     * @return Result.Success with list of schedules for the pond,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getSchedulesByPond(pondId: Long): Result<List<FeedingSchedule>>

    /**
     * Get schedule by ID
     *
     * @param scheduleId The ID of the schedule
     * @return Result.Success with schedule or null if not found,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getScheduleById(scheduleId: Long): Result<FeedingSchedule?>

    /**
     * Create a new feeding schedule
     *
     * @param name The name/title of the schedule
     * @param pondId The ID of the pond this schedule is for
     * @param pondName The name of the pond
     * @param startDate The date when feeding schedule starts
     * @param endDate The date when feeding schedule ends
     * @param feedingTime The time of day for feeding
     * @return Result.Success with the generated schedule ID if created successfully,
     *         Result.Error with message if creation failed
     */
    suspend fun createSchedule(
        name: String,
        pondId: Long,
        pondName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        feedingTime: LocalTime
    ): Result<Long>

    /**
     * Update an existing feeding schedule
     *
     * @param schedule The schedule with updated information (must have valid ID)
     * @return Result.Success with Unit if updated successfully,
     *         Result.Error with message if update failed
     */
    suspend fun updateSchedule(schedule: FeedingSchedule): Result<Unit>

    /**
     * Delete a feeding schedule
     *
     * @param scheduleId The ID of the schedule to delete
     * @return Result.Success with Unit if deleted successfully,
     *         Result.Error with message if deletion failed
     */
    suspend fun deleteSchedule(scheduleId: Long): Result<Unit>

    /**
     * Toggle schedule active status
     *
     * @param scheduleId The ID of the schedule
     * @param isActive The new active status
     * @return Result.Success with Unit if updated successfully,
     *         Result.Error with message if update failed
     */
    suspend fun toggleScheduleActive(scheduleId: Long, isActive: Boolean): Result<Unit>

    /**
     * Get schedules that are currently valid (within date range and active)
     *
     * @return Result.Success with list of current schedules,
     *         Result.Error with message if retrieval failed
     */
    suspend fun getCurrentSchedules(): Result<List<FeedingSchedule>>

    /**
     * Observe schedules that are currently valid
     * Emits updates whenever current schedules change
     *
     * @return Flow that emits list of current schedules
     */
    fun observeCurrentSchedules(): Flow<List<FeedingSchedule>>
}
