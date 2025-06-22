package com.xwurfel.tourry.feature.location

import android.content.Context
import android.location.Location
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DemoLocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val _locationUpdates = MutableSharedFlow<Location>()
    val locationUpdates: SharedFlow<Location> = _locationUpdates.asSharedFlow()

    // Demo simulation state
    private var simulationJob: Job? = null
    private var tourStops: List<DemoTourStop> = emptyList()
    private var currentStopIndex = 0
    private var isSimulationActive = false

    // Demo configuration
    private val movementSpeed = 5.0 // meters per second (walking speed)
    private val updateInterval = 1000L // milliseconds between location updates
    private val stopDwellTime = 8000L // time to spend at each stop in milliseconds
    private val startingOffset = 100.0 // meters away from first stop to start simulation

    suspend fun startLocationUpdates(
        tourId: String? = null,
        tourTitle: String = "Demo Tour"
    ): Result<Unit> {
        return try {
            Timber.d("🎭 Demo: Starting location tracking for tour: $tourTitle")

            // Simulate service connection
            _isServiceConnected.value = true

            // Start the simulation if we have tour stops
            if (tourStops.isNotEmpty()) {
                startLocationSimulation()
                _isTracking.value = true
                Timber.d("✅ Demo: Location tracking started successfully")
                Result.success(Unit)
            } else {
                Timber.w("⚠️ Demo: No tour stops available for simulation")
                Result.failure(Exception("No tour stops available for location simulation"))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to start location tracking")
            _isTracking.value = false
            _isServiceConnected.value = false
            Result.failure(e)
        }
    }

    fun stopLocationUpdates() {
        try {
            Timber.d("🎭 Demo: Stopping location tracking")

            stopLocationSimulation()
            _isTracking.value = false
            _isServiceConnected.value = false

            Timber.d("✅ Demo: Location tracking stopped")
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Error stopping location tracking")
        }
    }

    fun hasLocationPermission(): Boolean {
        // For demo purposes, always return true
        return true
    }

    fun hasBackgroundLocationPermission(): Boolean {
        // For demo purposes, always return true
        return true
    }

    suspend fun getLastKnownLocation(): Location? {
        return if (tourStops.isNotEmpty()) {
            createLocationForCoordinates(
                tourStops.first().latitude,
                tourStops.first().longitude
            )
        } else null
    }

    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    fun isLocationEnabled(): Boolean {
        // For demo purposes, always return true
        return true
    }

    fun unbindFromLocationService() {
        // No-op for demo
    }

    /**
     * Setup tour stops for the simulation
     */
    fun setupTourStops(stops: List<DemoTourStop>) {
        Timber.d("🎭 Demo: Setting up ${stops.size} tour stops for simulation")
        tourStops = stops.sortedBy { it.order }
        currentStopIndex = 0

        // If tracking is already active, restart simulation with new stops
        if (_isTracking.value) {
            stopLocationSimulation()
            startLocationSimulation()
        }
    }

    private fun startLocationSimulation() {
        if (isSimulationActive || tourStops.isEmpty()) return

        stopLocationSimulation()
        isSimulationActive = true
        currentStopIndex = 0

        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            Timber.d("🎭 Demo: Starting location simulation with ${tourStops.size} stops")

            // Start at an offset from the first stop to simulate approaching
            val firstStop = tourStops.first()
            val startingLocation = calculateOffsetLocation(
                firstStop.latitude,
                firstStop.longitude,
                startingOffset,
                225.0 // Southwest direction
            )

            emitLocation(startingLocation.first, startingLocation.second)
            delay(2000) // Initial delay

            // Move through each tour stop
            for (stopIndex in tourStops.indices) {
                if (!isSimulationActive) break

                val targetStop = tourStops[stopIndex]
                currentStopIndex = stopIndex

                // Move to the stop
                moveToLocation(targetStop.latitude, targetStop.longitude)

                // Dwell at the stop
                if (isSimulationActive) {
                    Timber.d("🎯 Demo: Arrived at stop ${stopIndex + 1}: ${targetStop.name}")
                    delay(stopDwellTime)
                }
            }

            // Simulation complete
            if (isSimulationActive) {
                Timber.d("🏁 Demo: Tour simulation completed!")
                delay(3000) // Final delay before stopping
            }
        }
    }

    private fun stopLocationSimulation() {
        isSimulationActive = false
        simulationJob?.cancel()
        simulationJob = null
    }

    private suspend fun moveToLocation(targetLat: Double, targetLon: Double) {
        val currentLocation = _locationUpdates.replayCache.lastOrNull()
        val startLat = currentLocation?.latitude ?: targetLat
        val startLon = currentLocation?.longitude ?: targetLon

        val distance = calculateDistance(startLat, startLon, targetLat, targetLon)
        val movementTime = (distance / movementSpeed * 1000).toLong() // Convert to milliseconds
        val steps = (movementTime / updateInterval).toInt().coerceAtLeast(1)

        Timber.d("🚶 Demo: Moving ${distance.toInt()}m to next location over ${movementTime / 1000}s")

        for (step in 0..steps) {
            if (!isSimulationActive) break

            val progress = step.toFloat() / steps
            val currentLat = startLat + (targetLat - startLat) * progress
            val currentLon = startLon + (targetLon - startLon) * progress

            emitLocation(currentLat, currentLon)

            if (step < steps) {
                delay(updateInterval)
            }
        }
    }

    private suspend fun emitLocation(latitude: Double, longitude: Double) {
        val location = createLocationForCoordinates(latitude, longitude)
        _locationUpdates.emit(location)

        Timber.v("📍 Demo: Location update - ${latitude.format(6)}, ${longitude.format(6)}")
    }

    private fun createLocationForCoordinates(latitude: Double, longitude: Double): Location {
        return Location("demo").apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = 10f // 10 meter accuracy
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

            // Add some realistic movement data
            this.speed = movementSpeed.toFloat()
            this.bearing = 0f // Could be calculated based on movement direction
        }
    }

    private fun calculateOffsetLocation(
        lat: Double,
        lon: Double,
        offsetMeters: Double,
        bearingDegrees: Double
    ): Pair<Double, Double> {
        val earthRadius = 6371000.0 // Earth's radius in meters
        val bearingRadians = Math.toRadians(bearingDegrees)
        val latRadians = Math.toRadians(lat)
        val lonRadians = Math.toRadians(lon)

        val newLatRadians = Math.asin(
            Math.sin(latRadians) * Math.cos(offsetMeters / earthRadius) +
                    Math.cos(latRadians) * Math.sin(offsetMeters / earthRadius) * Math.cos(
                bearingRadians
            )
        )

        val newLonRadians = lonRadians + Math.atan2(
            Math.sin(bearingRadians) * Math.sin(offsetMeters / earthRadius) * Math.cos(latRadians),
            Math.cos(offsetMeters / earthRadius) - Math.sin(latRadians) * Math.sin(newLatRadians)
        )

        return Pair(Math.toDegrees(newLatRadians), Math.toDegrees(newLonRadians))
    }

    // Extension function for formatting coordinates
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}

/**
 * Data class representing a tour stop for demo simulation
 */
data class DemoTourStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int
)