package com.example.aquaforecast.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feeding_schedule",
    foreignKeys = [
        ForeignKey(
            entity = PondEntity::class,
            parentColumns = ["id"],
            childColumns = ["pond_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pond_id"])]
)
data class FeedingScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "pond_id")
    val pondId: Long,
    @ColumnInfo(name = "pond_name")
    val pondName: String,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "end_date")
    val endDate: Long,
    @ColumnInfo(name = "feeding_time")
    val feedingTime: String, // Stored as HH:mm format
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
