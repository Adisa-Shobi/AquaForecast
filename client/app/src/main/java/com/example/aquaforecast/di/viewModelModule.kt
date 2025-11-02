package com.example.aquaforecast.di

import com.example.aquaforecast.ui.auth.AuthViewModel
import com.example.aquaforecast.ui.dashboard.DashboardViewModel
import com.example.aquaforecast.ui.dataentry.EntryViewModel
import com.example.aquaforecast.ui.datahistory.DataHistoryViewModel
import com.example.aquaforecast.ui.pondmanagement.PondFormViewModel
import com.example.aquaforecast.ui.pondmanagement.PondManagementViewModel
import com.example.aquaforecast.ui.schedule.ScheduleViewModel
import com.example.aquaforecast.ui.settings.SettingsViewModel
import com.example.aquaforecast.ui.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::EntryViewModel)
    viewModelOf(::DataHistoryViewModel)
    viewModelOf(::ScheduleViewModel)
    viewModelOf(::PondManagementViewModel)
    viewModelOf(::PondFormViewModel)
}