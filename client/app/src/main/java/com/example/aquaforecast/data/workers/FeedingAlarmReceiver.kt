package com.example.aquaforecast.data.workers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.aquaforecast.R
import com.example.aquaforecast.domain.repository.FeedingScheduleRepository
import com.example.aquaforecast.domain.repository.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver for feeding alarm notifications
 * Handles:
 * - Alarm triggers at exact feeding times
 * - Device boot to reschedule alarms
 * - Waking up the device for notifications
 */
class FeedingAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val feedingScheduleRepository: FeedingScheduleRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "FeedingAlarmReceiver triggered: ${intent.action}")

        when (intent.action) {
            ACTION_FEEDING_ALARM -> {
                handleFeedingAlarm(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
        }
    }

    private fun handleFeedingAlarm(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val scheduleName = intent.getStringExtra(EXTRA_SCHEDULE_NAME) ?: "Feeding Time"
        val pondName = intent.getStringExtra(EXTRA_POND_NAME) ?: "Your Pond"

        Log.d(TAG, "Handling feeding alarm for schedule: $scheduleId - $scheduleName")

        // Acquire wake lock to ensure device wakes up
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AquaForecast:FeedingAlarm"
        )

        try {
            wakeLock.acquire(30 * 1000L) // 30 seconds

            // Verify schedule is still active before showing notification
            CoroutineScope(Dispatchers.IO).launch {
                feedingScheduleRepository.getScheduleById(scheduleId)
                    .onSuccess { schedule ->
                        if (schedule != null && schedule.isActive && schedule.isCurrentlyActive()) {
                            sendNotification(context, scheduleId, scheduleName, pondName)

                            // Reschedule for tomorrow
                            ExactAlarmScheduler(context).scheduleNextAlarm(schedule)
                        } else {
                            Log.d(TAG, "Schedule $scheduleId is no longer active, not showing notification")
                        }
                    }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Device booted, rescheduling all active feeding alarms")

        // Reschedule all active feeding schedules
        CoroutineScope(Dispatchers.IO).launch {
            feedingScheduleRepository.observeAllSchedules().collect { schedules ->
                val scheduler = ExactAlarmScheduler(context)
                schedules.filter { it.isActive && it.isCurrentlyActive() }.forEach { schedule ->
                    scheduler.scheduleNextAlarm(schedule)
                }
            }
        }
    }

    private fun sendNotification(
        context: Context,
        scheduleId: Long,
        scheduleName: String,
        pondName: String
    ) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for when notification is tapped
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🐟 Feeding Time: $scheduleName")
            .setContentText("Time to feed $pondName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(scheduleId.toInt(), notification)
        Log.d(TAG, "Notification sent for schedule: $scheduleName")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Feeding Reminders"
            val descriptionText = "Notifications for fish feeding schedules"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_FEEDING_ALARM = "com.example.aquaforecast.FEEDING_ALARM"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_SCHEDULE_NAME = "schedule_name"
        const val EXTRA_POND_NAME = "pond_name"
        const val CHANNEL_ID = "feeding_reminders"
        private const val TAG = "FeedingAlarmReceiver"
    }
}
