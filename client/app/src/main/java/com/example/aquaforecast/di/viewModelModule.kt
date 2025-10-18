package com.example.aquaforecast.di

import com.example.aquaforecast.ui.auth.AuthViewModel
import com.example.aquaforecast.ui.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::SplashViewModel)
}