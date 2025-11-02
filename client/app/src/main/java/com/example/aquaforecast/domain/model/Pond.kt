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
    val createdAt: Long = System.currentTimeMillis()
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
        val growthPeriodDays = when (species) {
            Species.TILAPIA -> 180L
            Species.CATFISH -> 150L
        }
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
    TILAPIA("Tilapia", "Oreochromis niloticus"),
    CATFISH("Catfish", "Clarias gariepinus");

    companion object {
        fun fromString(value: String): Species {
            return when (value.uppercase()) {
                "TILAPIA" -> TILAPIA
                "CATFISH" -> CATFISH
                else -> throw IllegalArgumentException("Unknown species: $value")
            }
        }

        fun getAllSpecies(): List<Species> = entries.toList()
    }

    override fun toString(): String = displayName
}