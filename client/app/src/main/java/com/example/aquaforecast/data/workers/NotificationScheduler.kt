package com.example.aquaforecast.data.workers

import android.content.Context
import android.util.Log
import com.example.aquaforecast.domain.model.FeedingSchedule

/**
 * Handles scheduling and canceling of feeding reminder notifications
 * Uses ExactAlarmScheduler for precise, wake-up enabled notifications
 */
class NotificationScheduler(private val context: Context) {

    private val exactAlarmScheduler = ExactAlarmScheduler(context)

    /**
     * Schedule exact alarm for a feeding schedule
     * Uses AlarmManager for:
     * - Exact timing (not affected by Doze mode)
     * - Device wake-up capability
     * - Works even with low battery
     */
    fun scheduleNotifications(schedule: FeedingSchedule) {
        // Cancel existing notifications for this schedule
        cancelNotifications(schedule.id)

        // Don't schedule if inactive or out of date range
        if (!schedule.isActive || !schedule.isCurrentlyActive()) {
            Log.d(TAG, "Not scheduling notifications for ${schedule.name} - not active or out of date range")
            return
        }

        // Schedule using exact alarm
        exactAlarmScheduler.scheduleNextAlarm(schedule)
        Log.d(TAG, "Exact alarm scheduled for ${schedule.name} at ${schedule.getFormattedTime()}")
    }

    /**
     * Cancel all notifications for a specific schedule
     */
    fun cancelNotifications(scheduleId: Long) {
        exactAlarmScheduler.cancelAlarm(scheduleId)
        Log.d(TAG, "Cancelled alarm for schedule ID: $scheduleId")
    }

    companion object {
        private const val TAG = "NotificationScheduler"
    }
}
