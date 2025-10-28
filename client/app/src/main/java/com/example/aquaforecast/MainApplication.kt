package com.example.aquaforecast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.aquaforecast.data.local.DatabaseBootstrapper
import com.example.aquaforecast.data.workers.FeedingReminderWorker
import com.example.aquaforecast.di.bootstrapModule
import com.example.aquaforecast.di.dataStoreModule
import com.example.aquaforecast.di.databaseModule
import com.example.aquaforecast.di.mlModule
import com.example.aquaforecast.di.networkModule
import com.example.aquaforecast.di.repositoryModule
import com.example.aquaforecast.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val bootstrapper: DatabaseBootstrapper by inject()

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for feeding reminders
        createNotificationChannel()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@MainApplication)
            // Load modules
            modules(listOf(
                dataStoreModule,
                databaseModule,
                networkModule,
                repositoryModule,
                mlModule,
                viewModelModule,
                bootstrapModule
            ))
        }

        // Bootstrap database with sample data (debug only)
        if (BuildConfig.ENABLE_DATABASE_BOOTSTRAP) {
            applicationScope.launch {
                bootstrapper.bootstrap()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Feeding Reminders"
            val descriptionText = "Notifications for fish feeding schedules"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                FeedingReminderWorker.CHANNEL_ID,
                name,
                importance
            ).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}