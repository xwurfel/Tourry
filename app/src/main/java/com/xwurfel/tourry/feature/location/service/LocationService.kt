package com.xwurfel.tourry.feature.location.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.xwurfel.tourry.R
import com.xwurfel.tourry.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val binder = LocationBinder()

    // SharedFlow with replay=1 to cache the latest location for new collectors
    private val _locationUpdates = MutableSharedFlow<Location>(
        replay = 1,
        extraBufferCapacity = 0
    )
    val locationUpdates: SharedFlow<Location> = _locationUpdates.asSharedFlow()

    private var currentTourId: String? = null
    private var currentTourTitle: String = "Live Tour"
    private var isTrackingLocation = false
    private var lastKnownLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                lastKnownLocation = location
                val emitResult = _locationUpdates.tryEmit(location)
                Timber.d("Location update: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m) - Emitted: $emitResult")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Timber.d("LocationService created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_SERVICE -> {
                val tourId = intent.getStringExtra(EXTRA_TOUR_ID)
                val tourTitle = intent.getStringExtra(EXTRA_TOUR_TITLE) ?: "Live Tour"
                startLocationTracking(tourId, tourTitle)
            }

            ACTION_STOP_LOCATION_SERVICE -> {
                stopLocationTracking()
            }
        }
        return START_NOT_STICKY
    }

    fun startLocationTracking(tourId: String? = null, tourTitle: String = "Live Tour"): Boolean {
        if (isTrackingLocation) {
            Timber.d("Location tracking already active")
            return true
        }

        if (!hasRequiredPermissions()) {
            Timber.e("Cannot start location tracking: missing permissions")
            return false
        }

        if (!isLocationEnabled()) {
            Timber.e("Location services are disabled")
            return false
        }

        try {
            currentTourId = tourId
            currentTourTitle = tourTitle
            isTrackingLocation = true

            val notification = createNotification(tourTitle)

            // Start foreground service
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= 29) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
            )

            // Create location request with appropriate settings for live tours
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL
            ).apply {
                setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE)
                setMaxUpdateDelayMillis(MAX_UPDATE_DELAY)
                setWaitForAccurateLocation(false)
                setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL)
            }.build()

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            Timber.d("Location tracking started for tour: $tourTitle")
            return true

        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException when starting location tracking")
            stopLocationTracking()
            return false
        } catch (e: Exception) {
            Timber.e(e, "Exception when starting location tracking")
            stopLocationTracking()
            return false
        }
    }

    fun stopLocationTracking() {
        if (!isTrackingLocation) {
            Timber.d("Location tracking already stopped")
            return
        }

        isTrackingLocation = false
        currentTourId = null

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException when stopping location updates")
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        Timber.d("Location tracking stopped")
    }

    fun getLastKnownLocation(): Location? = lastKnownLocation

    fun isLocationTracking(): Boolean = isTrackingLocation

    private fun hasRequiredPermissions(): Boolean {
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasForegroundService = if (Build.VERSION.SDK_INT >= 34) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasFineLocation && hasForegroundService
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE)
                as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun createNotification(tourTitle: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text, tourTitle))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.location_tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.location_tracking_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLocationTracking()
        Timber.d("LocationService destroyed")
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    companion object {
        const val ACTION_START_LOCATION_SERVICE = "START_LOCATION_SERVICE"
        const val ACTION_STOP_LOCATION_SERVICE = "STOP_LOCATION_SERVICE"
        const val EXTRA_TOUR_ID = "TOUR_ID"
        const val EXTRA_TOUR_TITLE = "TOUR_TITLE"

        private const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 1001

        // Location update settings optimized for live tours
        private const val LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds for responsive tracking
        private const val MIN_UPDATE_INTERVAL = 1000L // 1 second minimum
        private const val MAX_UPDATE_DELAY = 5000L // 5 seconds maximum delay
        private const val MIN_UPDATE_DISTANCE = 3f // 3 meters to reduce noise

        fun getStartIntent(context: Context, tourId: String, tourTitle: String): Intent {
            return Intent(context, LocationService::class.java).apply {
                action = ACTION_START_LOCATION_SERVICE
                putExtra(EXTRA_TOUR_ID, tourId)
                putExtra(EXTRA_TOUR_TITLE, tourTitle)
            }
        }

        fun getStopIntent(context: Context): Intent {
            return Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP_LOCATION_SERVICE
            }
        }
    }
}