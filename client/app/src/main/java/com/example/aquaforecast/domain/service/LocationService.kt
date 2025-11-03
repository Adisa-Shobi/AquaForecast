package com.example.aquaforecast.domain.service

import android.location.Location
import com.example.aquaforecast.domain.repository.Result

/**
 * Service for capturing device location
 */
interface LocationService {
    /**
     * Get the current device location
     * @return Result.Success with Location if available, Result.Error with message if failed
     */
    suspend fun getCurrentLocation(): Result<Location?>

    /**
     * Get the last location used for data entry (from app preferences)
     * @return Result.Success with Location if available, Result.Error with message if failed
     */
    suspend fun getLastUsedLocation(): Result<Location?>

    /**
     * Save location as the last used location for data entry
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     */
    fun saveLastUsedLocation(latitude: Double, longitude: Double)

    /**
     * Check if location permissions are granted
     * @return true if either fine or coarse location permission is granted
     */
    fun hasLocationPermission(): Boolean
}
