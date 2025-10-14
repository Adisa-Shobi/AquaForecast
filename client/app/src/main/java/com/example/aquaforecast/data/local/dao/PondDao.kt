package com.example.aquaforecast.data.local.dao

import androidx.room.*
import com.example.aquaforecast.data.local.entity.PondEntity

@Dao
interface PondDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pond: PondEntity): Long

    @Query("SELECT * FROM pond LIMIT 1")
    suspend fun getPond(): PondEntity?

    @Update
    suspend fun update(pond: PondEntity)

    @Query("DELETE FROM pond")
    suspend fun deleteAll()
}