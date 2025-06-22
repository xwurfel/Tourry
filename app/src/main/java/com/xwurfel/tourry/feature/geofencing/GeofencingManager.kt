package com.xwurfel.tourry.feature.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GeofencingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val _geofenceEvents = MutableSharedFlow<GeofenceEvent>()
    val geofenceEvents: SharedFlow<GeofenceEvent> = _geofenceEvents.asSharedFlow()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    suspend fun addGeofencesForTour(tourStops: List<TourStopGeofence>): Result<Unit> {
        if (!hasLocationPermission()) {
            Timber.e("🚫 Location permission not granted for geofencing")
            return Result.failure(SecurityException("Location permission not granted"))
        }

        if (tourStops.isEmpty()) {
            Timber.w("⚠️ No tour stops provided for geofencing")
            return Result.failure(IllegalArgumentException("No tour stops provided"))
        }

        val geofences = tourStops.map { stop ->
            Timber.d("🎯 Creating geofence for stop: ${stop.id} at (${stop.latitude}, ${stop.longitude}) radius: ${stop.radius}m")

            Geofence.Builder()
                .setRequestId(stop.id)
                .setCircularRegion(stop.latitude, stop.longitude, stop.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        return try {
            suspendCancellableCoroutine { continuation ->
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener {
                        Timber.d("✅ Successfully added ${geofences.size} geofences")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(exception, "❌ Failed to add geofences")
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "🚫 Security exception when adding geofences")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "💥 Unexpected error when adding geofences")
            Result.failure(e)
        }
    }

    // FIXED: Make this function suspend as well
    suspend fun removeGeofences(geofenceIds: List<String>): Result<Unit> {
        return try {
            suspendCancellableCoroutine { continuation ->
                geofencingClient.removeGeofences(geofenceIds)
                    .addOnSuccessListener {
                        Timber.d("✅ Successfully removed ${geofenceIds.size} geofences")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(exception, "❌ Failed to remove geofences")
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIXED: Make this function suspend as well
    suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            suspendCancellableCoroutine { continuation ->
                geofencingClient.removeGeofences(geofencePendingIntent)
                    .addOnSuccessListener {
                        Timber.d("✅ Successfully removed all geofences")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(exception, "❌ Failed to remove all geofences")
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun handleGeofenceEvent(event: GeofenceEvent) {
        _geofenceEvents.emit(event)
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

data class TourStopGeofence(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 50f // Default 50 meters
)

sealed class GeofenceEvent {
    data class Enter(val geofenceId: String, val location: Location?) :
        GeofenceEvent()

    data class Exit(val geofenceId: String, val location: Location?) :
        GeofenceEvent()
}

// Extension functions for easier use
fun List<LiveTourStop>.toGeofences(): List<TourStopGeofence> {
    return map { stop ->
        TourStopGeofence(
            id = stop.id,
            latitude = stop.latitude,
            longitude = stop.longitude,
            radius = stop.geofenceRadius
        )
    }
}