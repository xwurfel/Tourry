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

/**
 * Utility class for creating sample tour data for testing
 */
object DemoTourDataFactory {

    /**
     * Create a sample tour with stops around Lviv city center for testing
     */
    fun createLvivCityTour(): List<LiveTourStop> {
        return listOf(
            LiveTourStop(
                id = "stop_1",
                name = "Rynok Square",
                description = "Historic market square in the heart of Lviv",
                latitude = 49.8416,
                longitude = 24.0317,
                order = 1,
                geofenceRadius = 30f,
                content = StopContent(
                    text = "Welcome to Rynok Square, the heart of historic Lviv!",
                    imageUrls = emptyList(),
                    audioUrl = null
                ),
                isVisited = false,
                isActive = false
            ),
            LiveTourStop(
                id = "stop_2",
                name = "Lviv City Hall",
                description = "Beautiful Gothic-Renaissance city hall",
                latitude = 49.8414,
                longitude = 24.0318,
                order = 2,
                geofenceRadius = 25f,
                content = StopContent(
                    text = "The iconic City Hall tower offers great views of the city.",
                    imageUrls = emptyList(),
                    audioUrl = null
                ),
                isVisited = false,
                isActive = false
            ),
            LiveTourStop(
                id = "stop_3",
                name = "Latin Cathedral",
                description = "Gothic Roman Catholic cathedral",
                latitude = 49.8419,
                longitude = 24.0312,
                order = 3,
                geofenceRadius = 35f,
                content = StopContent(
                    text = "This Gothic cathedral has stood here since the 14th century.",
                    imageUrls = emptyList(),
                    audioUrl = null
                ),
                isVisited = false,
                isActive = false
            ),
            LiveTourStop(
                id = "stop_4",
                name = "Armenian Cathedral",
                description = "Historic Armenian Apostolic cathedral",
                latitude = 49.8408,
                longitude = 24.0325,
                order = 4,
                geofenceRadius = 30f,
                content = StopContent(
                    text = "A beautiful example of Armenian architecture in Lviv.",
                    imageUrls = emptyList(),
                    audioUrl = null
                ),
                isVisited = false,
                isActive = false
            ),
            LiveTourStop(
                id = "stop_5",
                name = "Opera House",
                description = "Magnificent Lviv Opera and Ballet Theatre",
                latitude = 49.8434,
                longitude = 24.0258,
                order = 5,
                geofenceRadius = 40f,
                content = StopContent(
                    text = "One of the most beautiful opera houses in Europe.",
                    imageUrls = emptyList(),
                    audioUrl = null
                ),
                isVisited = false,
                isActive = false
            )
        )
    }

    /**
     * Create a longer walking tour for extended testing
     */
    fun createExtendedLvivTour(): List<LiveTourStop> {
        val basicTour = createLvivCityTour().toMutableList()

        basicTour.addAll(
            listOf(
                LiveTourStop(
                    id = "stop_6",
                    name = "Ivan Franko Park",
                    description = "Central park of Lviv",
                    latitude = 49.8383,
                    longitude = 24.0253,
                    order = 6,
                    geofenceRadius = 50f,
                    content = StopContent(
                        text = "A peaceful park perfect for relaxation.",
                        imageUrls = emptyList(),
                        audioUrl = null
                    ),
                    isVisited = false,
                    isActive = false
                ),
                LiveTourStop(
                    id = "stop_7",
                    name = "Potocki Palace",
                    description = "Historic palace, now an art gallery",
                    latitude = 49.8395,
                    longitude = 24.0267,
                    order = 7,
                    geofenceRadius = 30f,
                    content = StopContent(
                        text = "Beautiful palace housing an impressive art collection.",
                        imageUrls = emptyList(),
                        audioUrl = null
                    ),
                    isVisited = false,
                    isActive = false
                )
            )
        )

        return basicTour
    }
}