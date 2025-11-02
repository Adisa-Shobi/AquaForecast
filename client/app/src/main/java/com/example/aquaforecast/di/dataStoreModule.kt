package com.example.aquaforecast.di

import org.koin.dsl.module
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.aquaforecast.data.preferences.PreferencesManager
import org.koin.android.ext.koin.androidContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aquaforecast_preferences"
)

val dataStoreModule = module {
    single<DataStore<Preferences>> {
        androidContext().dataStore
    }

    single { PreferencesManager(dataStore = get()) }
}