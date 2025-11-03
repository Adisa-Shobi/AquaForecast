package com.example.aquaforecast.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.aquaforecast.data.preferences.LocationPreferences
import com.example.aquaforecast.domain.repository.Result
import com.example.aquaforecast.domain.repository.asError
import com.example.aquaforecast.domain.repository.asSuccess
import com.example.aquaforecast.domain.service.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "LocationServiceImpl"
private const val LOCATION_TIMEOUT_MS = 10000L // 10 seconds

/**
 * Implementation of LocationService using Google Play Services and app preferences
 */
class LocationServiceImpl(
    private val context: Context,
    private val locationPreferences: LocationPreferences
) : LocationService {

    override fun hasLocationPermission(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLocation || hasCoarseLocation
    }

    override suspend fun getCurrentLocation(): Result<Location?> = withContext(Dispatchers.IO) {
        try {
            // Check for location permissions
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                Log.w(TAG, "Location permissions not granted")
                return@withContext null.asSuccess() // Return null instead of error for graceful degradation
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            // Try to get current location with timeout
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                @Suppress("MissingPermission")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }

            if (location != null) {
                Log.d(TAG, "Location captured: ${location.latitude}, ${location.longitude}")
                location.asSuccess()
            } else {
                Log.d(TAG, "Current location unavailable")
                null.asSuccess()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting current location", e)
            null.asSuccess() // Return null for graceful degradation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null.asSuccess() // Return null for graceful degradation
        }
    }

    override suspend fun getLastUsedLocation(): Result<Location?> = withContext(Dispatchers.IO) {
        try {
            val savedLocation = locationPreferences.getLastUsedLocation()

            if (savedLocation != null) {
                val (latitude, longitude) = savedLocation
                val location = Location("app_preferences").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    time = locationPreferences.getLastUsedLocationTimestamp() ?: System.currentTimeMillis()
                }
                Log.d(TAG, "Last used location from preferences: $latitude, $longitude")
                location.asSuccess()
            } else {
                Log.d(TAG, "No last used location available in preferences")
                null.asSuccess()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last used location", e)
            null.asSuccess()
        }
    }

    override fun saveLastUsedLocation(latitude: Double, longitude: Double) {
        try {
            locationPreferences.saveLastUsedLocation(latitude, longitude)
            Log.d(TAG, "Saved last used location: $latitude, $longitude")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving last used location", e)
        }
    }
}
