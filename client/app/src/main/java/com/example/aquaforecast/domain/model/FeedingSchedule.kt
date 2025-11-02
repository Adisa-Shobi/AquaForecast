package com.example.aquaforecast.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class FeedingSchedule(
    val id: Long = 0,
    val name: String,
    val pondId: Long,
    val pondName: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    @Serializable(with = LocalTimeSerializer::class)
    val feedingTime: LocalTime,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedStartDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return startDate.format(formatter)
    }

    fun getFormattedEndDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return endDate.format(formatter)
    }

    fun getFormattedTime(): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return feedingTime.format(formatter)
    }

    fun isCurrentlyActive(): Boolean {
        val today = LocalDate.now()
        return isActive && !today.isBefore(startDate) && !today.isAfter(endDate)
    }

    fun getRemainingDays(): Int {
        val today = LocalDate.now()
        return if (today.isAfter(endDate)) {
            0
        } else {
            // Add 1 to include the end date in the count
            java.time.temporal.ChronoUnit.DAYS.between(today, endDate).toInt() + 1
        }
    }

    fun getDaysUntilStart(): Int {
        val today = LocalDate.now()
        return if (today.isBefore(startDate)) {
            java.time.temporal.ChronoUnit.DAYS.between(today, startDate).toInt()
        } else {
            0
        }
    }

    fun hasNotStarted(): Boolean {
        val today = LocalDate.now()
        return today.isBefore(startDate)
    }
}
