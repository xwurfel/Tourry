package com.xwurfel.tourry.feature.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofencingManager: GeofencingManager

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Timber.e("❌ Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggeringGeofences?.forEach { geofence ->
                    Timber.d("🎯 Geofence ENTER: ${geofence.requestId}")
                    geofencingManager.onGeofenceEvent(
                        GeofenceEvent.Enter(geofence.requestId)
                    )
                }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                triggeringGeofences?.forEach { geofence ->
                    Timber.d("🚪 Geofence EXIT: ${geofence.requestId}")
                    geofencingManager.onGeofenceEvent(
                        GeofenceEvent.Exit(geofence.requestId)
                    )
                }
            }

            else -> {
                Timber.w("⚠️ Unknown geofence transition: $geofenceTransition")
            }
        }
    }
}