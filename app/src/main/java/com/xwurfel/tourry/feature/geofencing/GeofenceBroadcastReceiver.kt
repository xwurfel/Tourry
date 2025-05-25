package com.xwurfel.tourry.feature.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofencingManager: GeofencingManager

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Timber.Forest.tag(TAG).e("Geofencing event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Timber.Forest.tag(TAG).e("Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val location = geofencingEvent.triggeringLocation

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                handleGeofenceEnter(triggeringGeofences, location)
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                handleGeofenceExit(triggeringGeofences, location)
            }

            else -> {
                Timber.Forest.tag(TAG).w("Unexpected geofence transition: $geofenceTransition")
            }
        }
    }

    private fun handleGeofenceEnter(geofences: List<Geofence>, location: Location?) {
        geofences.forEach { geofence ->
            Timber.Forest.tag(TAG).d("Entered geofence: ${geofence.requestId}")
            geofencingManager.handleGeofenceEvent(
                GeofenceEvent.Enter(geofence.requestId, location)
            )
        }
    }

    private fun handleGeofenceExit(geofences: List<Geofence>, location: Location?) {
        geofences.forEach { geofence ->
            Timber.Forest.tag(TAG).d("Exited geofence: ${geofence.requestId}")
            geofencingManager.handleGeofenceEvent(
                GeofenceEvent.Exit(geofence.requestId, location)
            )
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}