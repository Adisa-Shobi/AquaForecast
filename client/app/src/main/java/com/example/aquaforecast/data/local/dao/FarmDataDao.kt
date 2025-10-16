package com.example.aquaforecast.data.local.dao

import androidx.room.*
import com.example.aquaforecast.data.local.entity.FarmDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farmData: FarmDataEntity): Long

    @Query("SELECT * FROM farm_data ORDER BY timestamp DESC")
    fun getAll(): List<FarmDataEntity>

    @Query("SELECT * FROM farm_data WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<FarmDataEntity>

    @Query("SELECT * FROM farm_data WHERE isSynced = 0")
    suspend fun getUnsyncedData(): List<FarmDataEntity>

    @Query("UPDATE farm_data SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT * FROM farm_data WHERE pondId = :pondId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByPond(pondId: String): FarmDataEntity?

    @Query("SELECT * FROM farm_data WHERE pondId = :pondId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByPond(pondId: String, limit: Int): List<FarmDataEntity>

    @Query("DELETE FROM farm_data WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}