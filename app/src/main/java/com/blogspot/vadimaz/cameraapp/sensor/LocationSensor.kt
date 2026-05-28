package com.blogspot.vadimaz.cameraapp.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val hasLocation: Boolean = false
)

@SuppressLint("MissingPermission")
fun getLocationFlow(context: Context): Flow<LocationData> = callbackFlow {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            trySend(
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    hasLocation = true
                )
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    try {
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener
            )
        }
        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0f,
                listener
            )
        }

        // Initially attempt to get last known location
        val lastKnownGps = if (hasGps) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
        val lastKnownNetwork = if (hasNetwork) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
        val bestLocation = lastKnownGps ?: lastKnownNetwork
        if (bestLocation != null) {
            trySend(
                LocationData(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    altitude = bestLocation.altitude,
                    hasLocation = true
                )
            )
        }
    } catch (e: SecurityException) {
        // Location permission not granted
    } catch (e: Exception) {
        // Other potential errors
    }

    awaitClose {
        try {
            locationManager.removeUpdates(listener)
        } catch (e: Exception) {
            // Ignore on clean up failures
        }
    }
}
