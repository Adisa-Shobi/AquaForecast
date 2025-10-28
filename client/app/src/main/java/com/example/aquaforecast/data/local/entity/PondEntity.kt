package com.example.aquaforecast.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pond")
data class PondEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val species: String,
    @ColumnInfo(name = "stock_count")
    val stockCount: Int,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_harvested")
    val isHarvested: Boolean = false
)