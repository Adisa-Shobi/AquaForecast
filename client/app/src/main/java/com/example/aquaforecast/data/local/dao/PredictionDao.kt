package com.example.aquaforecast.data.local.dao

import androidx.room.*
import com.example.aquaforecast.data.local.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prediction: PredictionEntity): Long

    @Query("SELECT * FROM prediction WHERE pondId = :pondId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(pondId: String): PredictionEntity?

    @Query("SELECT * FROM prediction WHERE pondId = :pondId ORDER BY createdAt DESC")
    suspend fun getAllByPond(pondId: String): List<PredictionEntity>

    @Query("SELECT * FROM prediction WHERE pondId = :pondId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByPond(pondId: String, limit: Int): List<PredictionEntity>

    @Query("SELECT * FROM prediction WHERE pondId = :pondId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(pondId: String): Flow<PredictionEntity?>

    @Query("DELETE FROM prediction WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("SELECT * FROM prediction WHERE farmDataId = :farmDataId LIMIT 1")
    suspend fun getPredictionByFarmDataId(farmDataId: Long): PredictionEntity?
}