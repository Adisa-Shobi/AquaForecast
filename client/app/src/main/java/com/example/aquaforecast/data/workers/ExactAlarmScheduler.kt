package com.example.aquaforecast.data.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.aquaforecast.domain.model.FeedingSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules exact alarms for feeding reminders using AlarmManager
 * This ensures:
 * - Notifications fire at exact times
 * - Device wakes up even when sleeping
 * - Works even with battery saver mode
 */
class ExactAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule the next alarm for a feeding schedule
     * Calculates the next occurrence and sets an exact alarm
     */
    fun scheduleNextAlarm(schedule: FeedingSchedule) {
        // Don't schedule if inactive or out of date range
        if (!schedule.isActive || !schedule.isCurrentlyActive()) {
            Log.d(TAG, "Not scheduling alarm for ${schedule.name} - not active or out of date range")
            return
        }

        val nextTriggerTime = calculateNextTriggerTime(schedule)

        if (nextTriggerTime == null) {
            Log.d(TAG, "No valid trigger time for ${schedule.name}")
            return
        }

        // Create intent for the alarm
        val intent = Intent(context, FeedingAlarmReceiver::class.java).apply {
            action = FeedingAlarmReceiver.ACTION_FEEDING_ALARM
            putExtra(FeedingAlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(FeedingAlarmReceiver.EXTRA_SCHEDULE_NAME, schedule.name)
            putExtra(FeedingAlarmReceiver.EXTRA_POND_NAME, schedule.pondName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Schedule exact alarm that can wake device
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires permission check
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Exact alarm scheduled for ${schedule.name} at ${formatTime(nextTriggerTime)}")
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
            } else {
                // Pre-Android 12
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled for ${schedule.name} at ${formatTime(nextTriggerTime)}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm", e)
        }
    }

    /**
     * Cancel alarm for a specific schedule
     */
    fun cancelAlarm(scheduleId: Long) {
        val intent = Intent(context, FeedingAlarmReceiver::class.java).apply {
            action = FeedingAlarmReceiver.ACTION_FEEDING_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for schedule ID: $scheduleId")
        }
    }

    /**
     * Calculate the next trigger time in milliseconds
     * Returns null if schedule has ended
     */
    private fun calculateNextTriggerTime(schedule: FeedingSchedule): Long? {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        // Check if schedule has ended
        if (schedule.endDate.isBefore(today)) {
            return null
        }

        // Calculate today's trigger time
        var targetTime = LocalDateTime.of(
            today,
            schedule.feedingTime
        )

        // If today's time has passed, schedule for tomorrow
        if (targetTime.isBefore(now) || targetTime.isEqual(now)) {
            targetTime = targetTime.plusDays(1)

            // Check if tomorrow is past end date
            if (targetTime.toLocalDate().isAfter(schedule.endDate)) {
                return null
            }
        }

        // Make sure it's not before start date
        if (targetTime.toLocalDate().isBefore(schedule.startDate)) {
            targetTime = LocalDateTime.of(schedule.startDate, schedule.feedingTime)
        }

        return targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun formatTime(millis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
        return dateTime.toString()
    }

    companion object {
        private const val TAG = "ExactAlarmScheduler"
    }
}
