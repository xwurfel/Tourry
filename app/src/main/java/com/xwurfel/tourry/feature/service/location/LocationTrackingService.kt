package com.xwurfel.tourry.feature.service.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.MainActivity
import com.xwurfel.tourry.feature.tracking.domain.repository.GroupRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject
    lateinit var groupRepository: GroupRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private var groupId: String? = null
    private var userId: String? = null

    companion object {
        const val SERVICE_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"

        const val ACTION_START_TRACKING = "com.xwurfel.tourry.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.xwurfel.tourry.STOP_TRACKING"

        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"

        fun startTracking(context: Context, groupId: String, userId: String, groupName: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
            context.startForegroundService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    lifecycleScope.launch {
                        val currentGroupId = groupId
                        val currentUserId = userId

                        if (currentGroupId != null && currentUserId != null) {
                            groupRepository.updateLocation(
                                groupId = currentGroupId,
                                userId = currentUserId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val receivedGroupId = intent.getStringExtra(EXTRA_GROUP_ID)
                val receivedUserId = intent.getStringExtra(EXTRA_USER_ID)
                val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Tour"

                if (receivedGroupId != null && receivedUserId != null) {
                    startLocationUpdates(receivedGroupId, receivedUserId, groupName)
                }
            }

            ACTION_STOP_TRACKING -> {
                stopLocationUpdates()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startLocationUpdates(groupId: String, userId: String, groupName: String) {
        this.groupId = groupId
        this.userId = userId

        val notification = createNotification(groupName)
        startForeground(SERVICE_ID, notification)

        try {
            val locationRequest = LocationRequest.Builder(30000) // 30 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(15000) // 15 seconds
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            _isTracking.value = true
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val currentGroupId = groupId
        val currentUserId = userId

        if (currentGroupId != null && currentUserId != null) {
            groupRepository.stopLocationUpdates(currentGroupId, currentUserId)
        }

        groupId = null
        userId = null
        _isTracking.value = false
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.location_tracking_channel_name)
        val description = getString(R.string.location_tracking_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            this.description = description
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(groupName: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.tracking_notification_title))
        .setContentText(getString(R.string.tracking_notification_text, groupName))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setContentIntent(getLaunchPendingIntent())
        .build()

    private fun getLaunchPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}