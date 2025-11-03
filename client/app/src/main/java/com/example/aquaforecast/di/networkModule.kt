package com.example.aquaforecast.di

import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import com.example.aquaforecast.BuildConfig
import com.example.aquaforecast.data.remote.ApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit



val networkModule = module {
    // JSON configuration
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            prettyPrint = BuildConfig.DEBUG
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // Standard OkHttpClient for normal API requests
    single<OkHttpClient>(qualifier = org.koin.core.qualifier.named("standard")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    // Extended timeout OkHttpClient for sync operations (no timeout)
    single<OkHttpClient>(qualifier = org.koin.core.qualifier.named("sync")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(60, TimeUnit.SECONDS)     // Connection timeout: 1 minute
            .readTimeout(0, TimeUnit.SECONDS)         // No read timeout (batch sync can take time)
            .writeTimeout(0, TimeUnit.SECONDS)        // No write timeout (large payloads)
            .callTimeout(10, TimeUnit.MINUTES)        // Overall timeout: 10 minutes max
            .build()
    }

    // Standard Retrofit instance for most API calls
    single<Retrofit>(qualifier = org.koin.core.qualifier.named("standard")) {
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get(org.koin.core.qualifier.named("standard")))
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    // Retrofit instance with extended timeouts for sync operations
    single<Retrofit>(qualifier = org.koin.core.qualifier.named("sync")) {
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get(org.koin.core.qualifier.named("sync")))
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }

    // Standard ApiService
    single<ApiService>(qualifier = org.koin.core.qualifier.named("standard")) {
        get<Retrofit>(org.koin.core.qualifier.named("standard")).create(ApiService::class.java)
    }

    // ApiService with extended timeouts for sync
    single<ApiService>(qualifier = org.koin.core.qualifier.named("sync")) {
        get<Retrofit>(org.koin.core.qualifier.named("sync")).create(ApiService::class.java)
    }

    // Default ApiService (uses standard)
    single<ApiService> {
        get(org.koin.core.qualifier.named("standard"))
    }
}