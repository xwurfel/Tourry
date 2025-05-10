package com.xwurfel.tourry.feature.service.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGroupGeofence(
        groupId: String,
        center: LatLng,
        radiusMeters: Float,
        expirationTimeMillis: Long = Geofence.NEVER_EXPIRE
    ): Result<Unit> {
        return try {
            val geofenceId = "group_geofence_$groupId"

            val geofence = Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(center.latitude, center.longitude, radiusMeters)
                .setExpirationDuration(expirationTimeMillis)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    // Geofences added successfully
                }
                .addOnFailureListener { e ->
                    // Failed to add geofences
                    e.printStackTrace()
                }

            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateGroupGeofence(
        groupId: String,
        center: LatLng,
        radiusMeters: Float
    ): Result<Unit> {
        return try {
            // Remove existing geofence first
            removeGroupGeofence(groupId)

            // Add the new geofence
            addGroupGeofence(groupId, center, radiusMeters)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeGroupGeofence(groupId: String): Result<Unit> {
        return try {
            val geofenceId = "group_geofence_$groupId"

            geofencingClient.removeGeofences(listOf(geofenceId))
                .addOnSuccessListener {
                    // Geofence removed successfully
                }
                .addOnFailureListener { e ->
                    // Failed to remove geofence
                    e.printStackTrace()
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}