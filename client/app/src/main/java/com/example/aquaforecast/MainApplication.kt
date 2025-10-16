package com.example.aquaforecast

import android.app.Application
import com.example.aquaforecast.di.databaseModule
import com.example.aquaforecast.di.networkModule
import com.example.aquaforecast.di.repositoryModule
import com.example.aquaforecast.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MainApplication)
            // Load modules
            modules(listOf(
                databaseModule,
                networkModule,
                repositoryModule,
                viewModelModule
            ))
        }
    }
}