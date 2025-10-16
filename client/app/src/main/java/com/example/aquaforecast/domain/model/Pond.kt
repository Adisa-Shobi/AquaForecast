package com.example.aquaforecast.domain.model
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class Pond(
    val id: Long = 0,
    val name: String,
    val species: Species,
    val stockCount: Int,
    val startDate: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    val daysInOperation: Int
        get() {
            val diff = System.currentTimeMillis() - startDate
            return TimeUnit.MILLISECONDS.toDays(diff).toInt()
        }

    fun getFormattedStartDate(): String {
        val date = java.util.Date(startDate)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }

    fun getExpectedHarvestDate(): Long {
        val growthPeriodDays = when (species) {
            Species.TILAPIA -> 180  // ~6 months
            Species.CATFISH -> 150  // ~5 months
        }
        return startDate + TimeUnit.DAYS.toMillis(growthPeriodDays.toLong())
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