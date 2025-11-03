package com.example.aquaforecast.di

import com.example.aquaforecast.data.preferences.LocationPreferences
import com.example.aquaforecast.data.repository.AuthRepositoryImpl
import com.example.aquaforecast.data.repository.FarmDataRepositoryImpl
import com.example.aquaforecast.data.repository.FeedingScheduleRepositoryImpl
import com.example.aquaforecast.data.repository.ModelRepositoryImpl
import com.example.aquaforecast.data.repository.PondRepositoryImpl
import com.example.aquaforecast.data.repository.PredictionRepositoryImpl
import com.example.aquaforecast.data.repository.SyncRepositoryImpl
import com.example.aquaforecast.data.service.LocationServiceImpl
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.FeedingScheduleRepository
import com.example.aquaforecast.domain.repository.ModelRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.SyncRepository
import com.example.aquaforecast.domain.service.LocationService
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {

    single { FirebaseAuth.getInstance() }

    single { LocationPreferences(androidContext()) }

    single<LocationService> {
        LocationServiceImpl(
            context = androidContext(),
            locationPreferences = get()
        )
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            firebaseAuth = get(),
            apiService = get()
        )
    }
    singleOf(::FarmDataRepositoryImpl) {bind<FarmDataRepository>()}
    singleOf(::PondRepositoryImpl) {bind<PondRepository>()}
    singleOf(::PredictionRepositoryImpl) {bind<PredictionRepository>()}
    singleOf(::FeedingScheduleRepositoryImpl) {bind<FeedingScheduleRepository>()}

    single<ModelRepository> {
        ModelRepositoryImpl(
            apiService = get(),
            preferencesManager = get(),
            context = androidContext()
        )
    }

    single <SyncRepository>{ SyncRepositoryImpl(
        apiService = get(org.koin.core.qualifier.named("sync")), // Use sync-specific ApiService with extended timeouts
        farmDataDao = get(),
        predictionDao = get(),
        pondDao = get(),
        firebaseAuth = get(),
        context = androidContext()
    ) }
}