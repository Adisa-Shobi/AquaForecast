package com.example.aquaforecast.data.local.dao

import androidx.room.*
import com.example.aquaforecast.data.local.entity.FeedingScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for feeding schedules
 * Handles all database operations for feeding schedule data
 */
@Dao
interface FeedingScheduleDao {

    /**
     * Get all feeding schedules
     * Returns schedules ordered by start date descending
     */
    @Query("SELECT * FROM feeding_schedule ORDER BY start_date DESC")
    suspend fun getAllSchedules(): List<FeedingScheduleEntity>

    /**
     * Observe all feeding schedules
     * Emits updates whenever schedules change
     */
    @Query("SELECT * FROM feeding_schedule ORDER BY start_date DESC")
    fun observeAllSchedules(): Flow<List<FeedingScheduleEntity>>

    /**
     * Get active schedules
     * Returns only schedules where isActive is true
     */
    @Query("SELECT * FROM feeding_schedule WHERE is_active = 1 ORDER BY start_date DESC")
    suspend fun getActiveSchedules(): List<FeedingScheduleEntity>

    /**
     * Observe active schedules
     * Emits updates whenever active schedules change
     */
    @Query("SELECT * FROM feeding_schedule WHERE is_active = 1 ORDER BY start_date DESC")
    fun observeActiveSchedules(): Flow<List<FeedingScheduleEntity>>

    /**
     * Get schedules for a specific pond
     */
    @Query("SELECT * FROM feeding_schedule WHERE pond_id = :pondId ORDER BY start_date DESC")
    suspend fun getSchedulesByPond(pondId: Long): List<FeedingScheduleEntity>

    /**
     * Observe schedules for a specific pond
     */
    @Query("SELECT * FROM feeding_schedule WHERE pond_id = :pondId ORDER BY start_date DESC")
    fun observeSchedulesByPond(pondId: Long): Flow<List<FeedingScheduleEntity>>

    /**
     * Get schedule by ID
     */
    @Query("SELECT * FROM feeding_schedule WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): FeedingScheduleEntity?

    /**
     * Insert a new feeding schedule
     * Returns the row ID of the inserted schedule
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: FeedingScheduleEntity): Long

    /**
     * Update existing feeding schedule
     */
    @Update
    suspend fun update(schedule: FeedingScheduleEntity)

    /**
     * Delete a feeding schedule
     */
    @Delete
    suspend fun delete(schedule: FeedingScheduleEntity)

    /**
     * Delete schedule by ID
     */
    @Query("DELETE FROM feeding_schedule WHERE id = :scheduleId")
    suspend fun deleteById(scheduleId: Long)

    /**
     * Delete all schedules for a specific pond
     */
    @Query("DELETE FROM feeding_schedule WHERE pond_id = :pondId")
    suspend fun deleteByPondId(pondId: Long)

    /**
     * Delete all schedules
     */
    @Query("DELETE FROM feeding_schedule")
    suspend fun deleteAll()

    /**
     * Toggle schedule active status
     */
    @Query("UPDATE feeding_schedule SET is_active = :isActive WHERE id = :scheduleId")
    suspend fun updateActiveStatus(scheduleId: Long, isActive: Boolean)

    /**
     * Get schedules that are currently valid (within date range)
     */
    @Query("""
        SELECT * FROM feeding_schedule
        WHERE is_active = 1
        AND :currentDate >= start_date
        AND :currentDate <= end_date
        ORDER BY feeding_time ASC
    """)
    suspend fun getCurrentSchedules(currentDate: Long): List<FeedingScheduleEntity>

    /**
     * Observe schedules that are currently valid
     */
    @Query("""
        SELECT * FROM feeding_schedule
        WHERE is_active = 1
        AND :currentDate >= start_date
        AND :currentDate <= end_date
        ORDER BY feeding_time ASC
    """)
    fun observeCurrentSchedules(currentDate: Long): Flow<List<FeedingScheduleEntity>>

    /**
     * Get count of active schedules
     */
    @Query("SELECT COUNT(*) FROM feeding_schedule WHERE is_active = 1")
    suspend fun getActiveScheduleCount(): Int
}
