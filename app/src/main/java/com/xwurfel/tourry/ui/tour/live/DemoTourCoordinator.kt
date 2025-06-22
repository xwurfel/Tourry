package com.xwurfel.tourry.ui.tour.live

import com.xwurfel.tourry.feature.geofencing.DemoGeofencingManager
import com.xwurfel.tourry.feature.geofencing.TourStopGeofence
import com.xwurfel.tourry.feature.location.DemoLocationManager
import com.xwurfel.tourry.feature.location.DemoTourStop
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator class that manages demo simulation for tours.
 * This class bridges the gap between tour data and demo managers.
 */
@Singleton
class DemoTourCoordinator @Inject constructor(
    private val demoLocationManager: DemoLocationManager,
    private val demoGeofencingManager: DemoGeofencingManager
) {
    private var currentTourId: String? = null
    private var isSimulationRunning = false

    /**
     * Setup and start demo simulation for a tour
     */
    suspend fun startTourSimulation(
        tourId: String,
        tourTitle: String,
        tourStops: List<LiveTourStop>
    ): Result<Unit> {
        return try {
            Timber.d("🎭 Demo: Starting tour simulation for '$tourTitle'")

            currentTourId = tourId

            // Convert tour stops to demo format
            val demoStops = tourStops.map { stop ->
                DemoTourStop(
                    id = stop.id,
                    name = stop.name,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    order = stop.order
                )
            }

            val geofences = tourStops.map { stop ->
                TourStopGeofence(
                    id = stop.id,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    radius = stop.geofenceRadius
                )
            }

            // Setup demo managers
            demoLocationManager.setupTourStops(demoStops)
            demoGeofencingManager.addGeofencesForTour(geofences).getOrThrow()

            // Start location simulation
            demoLocationManager.startLocationUpdates(tourId, tourTitle).getOrThrow()

            isSimulationRunning = true
            Timber.d("✅ Demo: Tour simulation started successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to start tour simulation")
            Result.failure(e)
        }
    }

    /**
     * Stop the current tour simulation
     */
    suspend fun stopTourSimulation(): Result<Unit> {
        return try {
            Timber.d("🎭 Demo: Stopping tour simulation")

            demoLocationManager.stopLocationUpdates()
            demoGeofencingManager.removeAllGeofences().getOrThrow()

            isSimulationRunning = false
            currentTourId = null

            Timber.d("✅ Demo: Tour simulation stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to stop tour simulation")
            Result.failure(e)
        }
    }

    /**
     * Check if simulation is currently running
     */
    fun isSimulationActive(): Boolean = isSimulationRunning

    /**
     * Get current simulated tour ID
     */
    fun getCurrentTourId(): String? = currentTourId
}

/**
 * Demo configuration class for customizing simulation behavior
 */
data class DemoConfiguration(
    val movementSpeedMps: Double = 5.0, // meters per second
    val updateIntervalMs: Long = 1000L,   // milliseconds
    val stopDwellTimeMs: Long = 8000L,    // time spent at each stop
    val startingOffsetM: Double = 100.0,  // meters away from first stop
    val geofenceRadiusM: Double = 30.0,   // geofence detection radius
    val enableLogs: Boolean = true         // enable detailed logging
)

/**
 * Extension functions to help integrate demo managers with existing code
 */

/**
 * Convert LiveTourStop to DemoTourStop
 */
fun LiveTourStop.toDemoStop(): DemoTourStop {
    return DemoTourStop(
        id = this.id,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        order = this.order
    )
}

/**
 * Convert LiveTourStop to TourStopGeofence
 */
fun LiveTourStop.toGeofence(): TourStopGeofence {
    return TourStopGeofence(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        radius = this.geofenceRadius
    )
}

/**
 * Extension to setup demo simulation from a list of LiveTourStops
 */
suspend fun DemoTourCoordinator.setupFromLiveTourStops(
    tourId: String,
    tourTitle: String,
    stops: List<LiveTourStop>
): Result<Unit> {
    return this.startTourSimulation(tourId, tourTitle, stops)
}
