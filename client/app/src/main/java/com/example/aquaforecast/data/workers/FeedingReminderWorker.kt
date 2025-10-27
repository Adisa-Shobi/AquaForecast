package com.example.aquaforecast.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aquaforecast.R
import com.example.aquaforecast.domain.repository.FeedingScheduleRepository
import com.example.aquaforecast.domain.repository.onSuccess
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that sends feeding reminder notifications
 * Triggered at the scheduled feeding time for a specific schedule
 */
class FeedingReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val feedingScheduleRepository: FeedingScheduleRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()

            // Get schedule ID from input data
            val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1)

            if (scheduleId == -1L) {
                Log.e(TAG, "Invalid schedule ID")
                return Result.failure()
            }

            // Get schedule details and send notification
            feedingScheduleRepository.getScheduleById(scheduleId)
                .onSuccess { schedule ->
                    schedule?.let {
                        // Only send notification if schedule is active and currently running
                        if (it.isActive && it.isCurrentlyActive()) {
                            sendNotification(
                                scheduleId = it.id,
                                scheduleName = it.name,
                                pondName = it.pondName,
                                feedingTime = it.getFormattedTime()
                            )
                            Log.d(TAG, "Notification sent for schedule: ${it.name}")
                        } else {
                            Log.d(TAG, "Schedule ${it.name} is not active or not in date range")
                        }
                    } ?: Log.d(TAG, "Schedule with ID $scheduleId not found")
                }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Feeding Reminders"
            val descriptionText = "Notifications for fish feeding schedules"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(
        scheduleId: Long,
        scheduleName: String,
        pondName: String,
        feedingTime: String
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for when notification is tapped
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Feeding Time: $scheduleName")
            .setContentText("Time to feed $pondName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(scheduleId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "feeding_reminders"
        const val WORK_NAME_PREFIX = "feeding_reminder_"
        const val KEY_SCHEDULE_ID = "schedule_id"
        private const val TAG = "FeedingReminderWorker"
    }
}
