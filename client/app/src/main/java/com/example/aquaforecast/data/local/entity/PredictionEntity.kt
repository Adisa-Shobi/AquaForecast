package com.example.aquaforecast.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["pondId"])
    ]
)
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val predictedWeight: Double,
    val predictedLength: Double,
    val harvestDate: Long,
    val createdAt: Long,
    val pondId: String
)