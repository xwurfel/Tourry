package com.xwurfel.tourry.feature.service.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.xwurfel.tourry.feature.service.notification.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Check if the transition type is of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofences that were triggered
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId

                if (geofenceId.startsWith("group_geofence_")) {
                    val groupId = geofenceId.removePrefix("group_geofence_")

                    // Send a notification that the user has left the group area
                    notificationService.showGeofenceExitNotification(
                        context = context,
                        groupId = groupId
                    )
                }
            }
        }
    }
}