package com.xwurfel.tourry.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.xwurfel.tourry.feature.location.service.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private var locationService: LocationService? = null
    private var isServiceBound = false

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    // Expose service connection state
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            isServiceBound = true
            _isServiceConnected.value = true
            _isTracking.value = locationService?.isLocationTracking() ?: false
            Timber.d("LocationService connected - tracking: ${_isTracking.value}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isServiceBound = false
            _isServiceConnected.value = false
            _isTracking.value = false
            Timber.d("LocationService disconnected")
        }
    }

    /**
     * Flow of location updates from the LocationService.
     * This flow will be empty until the service is connected and location tracking is started.
     */
    val locationUpdates: Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        // Ensure service is bound
        if (!isServiceBound) {
            bindToLocationService()
        }

        // Wait for service connection
        _isServiceConnected.filterNotNull().collect { isConnected ->
            if (isConnected && locationService != null) {
                Timber.d("Service connected, starting to collect location updates")

                // Collect from the service's location updates flow
                locationService!!.locationUpdates.collect { location ->
                    Timber.d("LocationManager received location: ${location.latitude}, ${location.longitude}")
                    trySend(location)
                }
            }
        }

        awaitClose {
            // Don't unbind here as the service might be used by other components
            Timber.d("LocationManager location updates flow closed")
        }
    }

    /**
     * Start location updates by starting the LocationService
     */
    suspend fun startLocationUpdates(
        tourId: String? = null,
        tourTitle: String = "Live Tour"
    ): Result<Unit> {
        try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }

            // Start the service using intent to ensure it runs in foreground
            val intent = LocationService.getStartIntent(context, tourId ?: "", tourTitle)
            context.startForegroundService(intent)

            // Also bind to the service to get access to its methods and flows
            if (!isServiceBound) {
                bindToLocationService()
            }

            // Wait a bit for service to start up
            var attempts = 0
            while (!isServiceBound && attempts < 20) { // 2 seconds max wait
                kotlinx.coroutines.delay(100)
                attempts++
            }

            val service = locationService
            if (service != null) {
                val startResult = service.startLocationTracking(tourId, tourTitle)
                if (startResult) {
                    _isTracking.value = true
                    Timber.d("Location tracking started successfully")
                    return Result.success(Unit)
                } else {
                    return Result.failure(Exception("Failed to start location tracking in service"))
                }
            } else {
                return Result.failure(Exception("Could not connect to LocationService"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to start location updates")
            _isTracking.value = false
            return Result.failure(e)
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        try {
            // Stop the service
            val intent = LocationService.getStopIntent(context)
            context.startService(intent)

            locationService?.stopLocationTracking()
            _isTracking.value = false

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
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (
            e: Exception
        ) {
            null
        }
    }

    /**
     * Bind to the LocationService
     */
    private fun bindToLocationService() {
        if (isServiceBound) return

        val intent = Intent(context, LocationService::class.java)
        val bindResult = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Timber.d("Attempting to bind to LocationService: $bindResult")
    }

    /**
     * Unbind from the LocationService
     */
    fun unbindFromLocationService() {
        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
                isServiceBound = false
                _isServiceConnected.value = false
                Timber.d("Unbound from LocationService")
            } catch (e: Exception) {
                Timber.e(e, "Error unbinding from LocationService")
            }
        }
    }

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
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