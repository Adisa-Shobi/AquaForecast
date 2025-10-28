package com.example.aquaforecast.di

import com.example.aquaforecast.data.local.DatabaseBootstrapper
import org.koin.dsl.module

val bootstrapModule = module {
    single {
        DatabaseBootstrapper(
            dataStore = get(),
            pondDao = get(),
            farmDataDao = get(),
            predictionDao = get(),
            feedingScheduleDao = get()
        )
    }
}
