package com.example.aquaforecast.di

import com.example.aquaforecast.data.repository.AuthRepositoryImpl
import com.example.aquaforecast.data.repository.FarmDataRepositoryImpl
import com.example.aquaforecast.data.repository.PondRepositoryImpl
import com.example.aquaforecast.data.repository.PredictionRepositoryImpl
import com.example.aquaforecast.data.repository.SyncRepositoryImpl
import com.example.aquaforecast.domain.repository.AuthRepository
import com.example.aquaforecast.domain.repository.FarmDataRepository
import com.example.aquaforecast.domain.repository.PondRepository
import com.example.aquaforecast.domain.repository.PredictionRepository
import com.example.aquaforecast.domain.repository.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {

    single { FirebaseAuth.getInstance() }

    single<AuthRepository> {
        AuthRepositoryImpl(firebaseAuth = get())
    }
    singleOf(::FarmDataRepositoryImpl) {bind<FarmDataRepository>()}
    singleOf(::PondRepositoryImpl) {bind<PondRepository>()}
    singleOf(::PredictionRepositoryImpl) {bind<PredictionRepository>()}

    single <SyncRepository>{ SyncRepositoryImpl(
        apiService = get(),
        farmDataDao = get(),
        androidContext()
    ) }
}