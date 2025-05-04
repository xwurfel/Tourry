package com.xwurfel.tourry.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.service.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var routeRepository: RouteRepository

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
                handleGeofenceEnter(context, geofencingEvent.triggeringGeofences!!)
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
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
                            val routePoint = routeRepository.getRoutePointById(pointId)

                            if (routePoint != null) {
                                notificationService.showGeofenceEntryNotification(
                                    tourId = tourId,
                                    pointId = pointId,
                                    pointName = routePoint.title
                                )
                            } else {
                                notificationService.showGeofenceEntryNotification(
                                    tourId = tourId,
                                    pointId = pointId,
                                    pointName = "Tour Stop"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence enter", e)
            }
        }
    }

    private fun handleGeofenceExit(context: Context, triggeringGeofences: List<Geofence>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (geofence in triggeringGeofences) {
                    val geofenceId = geofence.requestId
                    Log.d(TAG, "User exited geofence: $geofenceId")

                    val idParts = geofenceId.split("_")
                    if (idParts.size >= 3 && idParts[0] == "TOURRY") {
                        val pointId = idParts[2].toLongOrNull()

                        if (pointId != null) {
                            val routePoint = routeRepository.getRoutePointById(pointId)

                            if (routePoint != null) {
                                notificationService.showGeofenceExitNotification(routePoint.title)
                            } else {
                                notificationService.showGeofenceExitNotification("Tour Stop")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence exit", e)
            }
        }
    }
}