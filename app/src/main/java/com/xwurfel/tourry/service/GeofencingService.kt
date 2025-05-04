package com.xwurfel.tourry.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.receiver.GeofenceBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofencingService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GeofencingService"
        private const val DEFAULT_RADIUS_METERS = 100f
        private const val GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE
    }

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

    fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Adds geofences for a list of route points. Each point will create a circular geofence.
     *
     * @param routePoints List of route points to create geofences for
     * @param tourId The ID of the tour these points belong to (used in geofence IDs)
     * @return Result indicating success or failure
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun addGeofencesForRoutePoints(
        routePoints: List<RoutePoint>,
        tourId: Long
    ): Result<Unit> {
        if (!hasRequiredPermissions()) {
            return Result.failure(SecurityException("Missing location permission"))
        }

        if (routePoints.isEmpty()) {
            return Result.failure(IllegalArgumentException("No route points provided"))
        }

        try {
            val geofenceList = routePoints.map { point ->
                Geofence.Builder()
                    .setRequestId("TOURRY_${tourId}_${point.id}")
                    .setCircularRegion(
                        point.location.latitude,
                        point.location.longitude,
                        DEFAULT_RADIUS_METERS
                    )
                    .setExpirationDuration(GEOFENCE_EXPIRATION)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                                Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
            }

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            // TODO: fix Unresolved reference 'await'.
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)//.await()
            Log.d(TAG, "Successfully added ${geofenceList.size} geofences for tour $tourId")
            return Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding geofences", e)
            return Result.failure(e)
        }
    }

    /**
     * Removes geofences for a specific tour.
     *
     * @param tourId The ID of the tour to remove geofences for
     * @return Result indicating success or failure
     */
    suspend fun removeGeofencesForTour(tourId: Long): Result<Unit> {
        try {
            // TODO: check this
            // We need to get the existing geofence IDs that match this tour ID
            // For simplicity, we'll just remove all geofences
            geofencingClient.removeGeofences(geofencePendingIntent)//.await() Unresolved reference 'await'.
            Log.d(TAG, "Removed all geofences for tour $tourId")
            return Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofences", e)
            return Result.failure(e)
        }
    }

    suspend fun removeAllGeofences(): Result<Unit> {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)// .await() Unresolved reference 'await'.
            Log.d(TAG, "Removed all geofences")
            return Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error removing all geofences", e)
            return Result.failure(e)
        }
    }
}