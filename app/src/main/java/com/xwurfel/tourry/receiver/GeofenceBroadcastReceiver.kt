package com.xwurfel.tourry.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val NOTIFICATION_CHANNEL_ID = "tourry_geofence_channel"
        private const val NOTIFICATION_ID = 1001
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GeofencingEvent.fromIntent(intent) ?: return
        } else {
            @Suppress("DEPRECATION")
            GeofencingEvent.fromIntent(intent) ?: return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(
                geofencingEvent.errorCode
            )
            Log.e(TAG, "Geofence error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                requireNotNull(geofencingEvent.triggeringGeofences)
                handleGeofenceEnter(context, geofencingEvent.triggeringGeofences!!)
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                requireNotNull(geofencingEvent.triggeringGeofences)

                handleGeofenceExit(context, geofencingEvent.triggeringGeofences!!)
            }

            else -> {
                Log.e(TAG, "Invalid geofence transition type: $geofenceTransition")
            }
        }
    }

    private fun handleGeofenceEnter(context: Context, triggeringGeofences: List<Geofence>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (geofence in triggeringGeofences) {
                    val geofenceId = geofence.requestId
                    Log.d(TAG, "User entered geofence: $geofenceId")

                    // Parse the geofence ID to extract tour and point IDs
                    // Format: TOURRY_tourId_pointId
                    val idParts = geofenceId.split("_")
                    if (idParts.size >= 3 && idParts[0] == "TOURRY") {
                        val tourId = idParts[1].toLongOrNull()
                        val pointId = idParts[2].toLongOrNull()

                        if (tourId != null && pointId != null) {
                            // Get tour and point info to use in notification
                            // For now, we'll just use the IDs in the notification
                            showNotification(
                                context,
                                "You've reached a tour stop!",
                                "You've arrived at a point of interest. Tap to check in."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence enter", e)
            }
        }
    }

    private fun handleGeofenceExit(context: Context, triggeringGeofences: List<Geofence>) {
        // Similar to enter, but for exit events
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (geofence in triggeringGeofences) {
                    val geofenceId = geofence.requestId
                    Log.d(TAG, "User exited geofence: $geofenceId")

                    // For now, we won't show a notification for exits, but you could add that here
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence exit", e)
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val name = "Tour Geofence Notifications"
        val descriptionText = "Notifications for tour check-in points"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }
}