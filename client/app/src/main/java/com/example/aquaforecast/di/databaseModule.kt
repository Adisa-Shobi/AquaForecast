package com.example.aquaforecast.di

import androidx.room.Room
import com.example.aquaforecast.data.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { Room.databaseBuilder(
        androidContext(),
        AppDatabase::class.java,
        "aquaforecast_db"
    )
    .fallbackToDestructiveMigration()
    .build()
    }

    single { get<AppDatabase>().farmDataDao() }
    single { get<AppDatabase>().pondDao() }
    single { get<AppDatabase>().predictionDao() }
    single { get<AppDatabase>().feedingScheduleDao() }
}