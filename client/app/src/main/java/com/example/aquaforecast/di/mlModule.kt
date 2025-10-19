package com.example.aquaforecast.di

import com.example.aquaforecast.data.ml.MLPredictor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mlModule = module {
    single {
        MLPredictor(
            context = androidContext(),
            farmDataRepository = get(),
            preferencesManager = get()
        )
    }
}
