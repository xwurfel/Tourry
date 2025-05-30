package com.xwurfel.tourry.feature.location

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.xwurfel.tourry.feature.location.service.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var locationService: LocationService? = null
    private var isServiceBound = false

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            isServiceBound = true
            Timber.d("LocationService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isServiceBound = false
            _isTracking.value = false
            Timber.d("LocationService disconnected")
        }
    }

    /**
     * Flow of location updates from the LocationService
     */
    val locationUpdates: Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        // Bind to location service if not already bound
        if (!isServiceBound) {
            bindToLocationService()
        }

        // Wait for service connection and start collecting location updates
        val locationService = waitForServiceConnection()
        if (locationService != null) {
            locationService.locationUpdates.collect { location ->
                trySend(location)
            }
        } else {
            close(IllegalStateException("Failed to connect to LocationService"))
        }

        awaitClose {
            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }
        }
    }

    /**
     * Start location updates with proper error handling
     */
    suspend fun startLocationUpdates() {
        try {
            if (!hasLocationPermission()) {
                throw SecurityException("Location permission not granted")
            }

            if (!isServiceBound) {
                bindToLocationService()
            }

            val service = waitForServiceConnection()
            if (service != null) {
                service.startLocationTracking(null, "Live Tour")
                _isTracking.value = true
                Timber.d("Location tracking started")
            } else {
                throw IllegalStateException("Failed to connect to LocationService")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start location updates")
            _isTracking.value = false
            throw e
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        try {
            locationService?.stopLocationTracking()
            _isTracking.value = false

            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }

            Timber.d("Location tracking stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping location updates")
        }
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for Android < 10
        }
    }

    /**
     * Get the last known location if available
     */
    fun getLastKnownLocation(): Location? {
        return locationService?.getLastKnownLocation()
    }

    private fun bindToLocationService() {
        val intent = Intent(context, LocationService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private suspend fun waitForServiceConnection(): LocationService? {
        // Wait for service connection with timeout
        var attempts = 0
        while (!isServiceBound && attempts < 10) {
            kotlinx.coroutines.delay(100)
            attempts++
        }
        return locationService
    }

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    /**
     * Check if location services are enabled on the device
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
}