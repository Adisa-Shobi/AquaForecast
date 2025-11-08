package com.example.aquaforecast.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class Pond(
    val id: Long = 0,
    val name: String,
    val species: Species,
    val stockCount: Int,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
    val isHarvested: Boolean = false
) {
    val daysInOperation: Int
        get() {
            return ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt()
        }

    fun getFormattedStartDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return startDate.format(formatter)
    }

    fun getExpectedHarvestDate(): LocalDate {
        val growthPeriodDays = 150L // African Catfish growth period
        return startDate.plusDays(growthPeriodDays)
    }

    fun getExpectedHarvestDateMillis(): Long {
        return getExpectedHarvestDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

@Serializable
enum class Species(val displayName: String, val scientificName: String) {
    CATFISH("African Catfish", "Clarias gariepinus");

    companion object {
        fun fromString(value: String): Species {
            return when (value.uppercase()) {
                "CATFISH" -> CATFISH
                else -> CATFISH // Default to African Catfish
            }
        }

        fun getAllSpecies(): List<Species> = entries.toList()
    }

    override fun toString(): String = displayName
}