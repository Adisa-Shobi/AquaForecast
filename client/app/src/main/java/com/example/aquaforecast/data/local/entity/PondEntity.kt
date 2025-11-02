package com.example.aquaforecast.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pond")
data class PondEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val species: String,
    val stockCount: Int,
    val startDate: Long,
    val createdAt: Long
)