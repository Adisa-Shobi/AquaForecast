package com.example.aquaforecast.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farm_data",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["pondId"]),
        Index(value = ["isSynced"])
    ]
    )
data class FarmDataEntity (
    @PrimaryKey(true)
    val id: Long = 0,
    val temperature: Double,
    val ph: Double,
    val dissolvedOxygen: Double,
    val ammonia: Double,
    val nitrate: Double,
    val turbidity: Double,
    val timestamp: Long,
    val pondId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isSynced: Boolean = false
)