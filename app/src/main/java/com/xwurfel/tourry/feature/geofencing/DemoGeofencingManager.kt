package com.xwurfel.tourry.feature.geofencing

import android.content.Context
import android.location.Location
import com.xwurfel.tourry.feature.location.DemoLocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoGeofencingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val demoLocationManager: DemoLocationManager
) {
    private val _geofenceEvents = MutableSharedFlow<GeofenceEvent>()
    val geofenceEvents: SharedFlow<GeofenceEvent> = _geofenceEvents.asSharedFlow()

    private var activeGeofences = mutableListOf<TourStopGeofence>()
    private var enteredGeofences = mutableSetOf<String>()
    private var simulationJob: Job? = null

    // Demo configuration
    private val geofenceDetectionRadius = 30.0 // meters
    private val geofenceCheckInterval = 2000L // milliseconds

    suspend fun addGeofencesForTour(tourStops: List<TourStopGeofence>): Result<Unit> {
        return try {
            Timber.d("🎭 Demo: Setting up geofences for ${tourStops.size} stops")

            activeGeofences.clear()
            activeGeofences.addAll(tourStops)
            enteredGeofences.clear()

            // Start monitoring location changes for geofence simulation
            startGeofenceSimulation()

            Timber.d("✅ Demo: Geofences setup complete")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to setup geofences")
            Result.failure(e)
        }
    }

    suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            Timber.d("🎭 Demo: Removing all geofences")

            // Exit all currently entered geofences
            enteredGeofences.toList().forEach { geofenceId ->
                _geofenceEvents.tryEmit(GeofenceEvent.Exit(geofenceId))
                Timber.d("🚪 Demo: Simulated exit from geofence: $geofenceId")
            }

            activeGeofences.clear()
            enteredGeofences.clear()
            stopGeofenceSimulation()

            Timber.d("✅ Demo: All geofences removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to remove geofences")
            Result.failure(e)
        }
    }

    fun onGeofenceEvent(event: GeofenceEvent) {
        // This method is kept for compatibility but not used in demo
        _geofenceEvents.tryEmit(event)
    }

    private fun startGeofenceSimulation() {
        stopGeofenceSimulation()

        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            demoLocationManager.locationUpdates.collect { location ->
                checkGeofenceTransitions(location)
            }
        }
    }

    private fun stopGeofenceSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private suspend fun checkGeofenceTransitions(currentLocation: Location) {
        activeGeofences.forEach { geofence ->
            val distance = calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                geofence.latitude,
                geofence.longitude
            )

            val isWithinGeofence = distance <= geofenceDetectionRadius
            val wasInGeofence = enteredGeofences.contains(geofence.id)

            when {
                // Entering geofence
                isWithinGeofence && !wasInGeofence -> {
                    enteredGeofences.add(geofence.id)
                    _geofenceEvents.tryEmit(GeofenceEvent.Enter(geofence.id))
                    Timber.d("🚪 Demo: Entered geofence: ${geofence.id} (distance: ${distance.toInt()}m)")
                }

                // Exiting geofence
                !isWithinGeofence && wasInGeofence -> {
                    enteredGeofences.remove(geofence.id)
                    _geofenceEvents.tryEmit(GeofenceEvent.Exit(geofence.id))
                    Timber.d("🚪 Demo: Exited geofence: ${geofence.id} (distance: ${distance.toInt()}m)")
                }
            }
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}

// Geofence events sealed class (should match the original)
sealed class GeofenceEvent {
    data class Enter(val geofenceId: String) : GeofenceEvent()
    data class Exit(val geofenceId: String) : GeofenceEvent()
}

// Tour stop geofence data class (should match the original)
data class TourStopGeofence(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
)