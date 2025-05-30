package com.xwurfel.tourry.ui.tour.live

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.audio.AudioPlayerManager
import com.xwurfel.tourry.feature.audio.PlaybackState
import com.xwurfel.tourry.feature.geofencing.GeofenceEvent
import com.xwurfel.tourry.feature.geofencing.GeofencingManager
import com.xwurfel.tourry.feature.location.LocationManager
import com.xwurfel.tourry.feature.mock.MockDataManager
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager,
    private val geofencingManager: GeofencingManager,
    private val mockDataManager: MockDataManager,
    private val audioPlayerManager: AudioPlayerManager,
    private val tourAnalytics: TourAnalytics,
) : MviViewModel<LiveTourUiState, LiveTourPartialState, LiveTourEvent, LiveTourIntent>(
    initialState = LiveTourUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        tourAnalytics.startTourSession(tourId, "", false)

        observeContinuousChanges(
            loadTourData(),
            observeLocationUpdates(),
            observeGeofenceEvents(),
            observeAudioPlayerState()
        )
    }

    override fun mapIntents(intent: LiveTourIntent): Flow<LiveTourPartialState> = flow {
        when (intent) {
            LiveTourIntent.StartLocationTracking -> {
                handleStartLocationTracking()
            }

            LiveTourIntent.PauseTour -> {
                tourAnalytics.trackTourPaused(tourId, "user_action")
                locationManager.stopLocationUpdates()
                emit(LiveTourPartialState.TourPaused)
            }

            LiveTourIntent.ResumeTour -> {
                locationManager.startLocationUpdates()
                emit(LiveTourPartialState.TourResumed)
            }

            LiveTourIntent.CompleteTour -> {
                handleTourCompletion()
            }

            LiveTourIntent.ExitTour -> {
                handleTourExit()
                publishEvent(LiveTourEvent.NavigateBack)
            }

            is LiveTourIntent.OnGeofenceEntered -> {
                handleGeofenceEntered(intent.stopId)
            }

            is LiveTourIntent.OnGeofenceExited -> {
                emit(LiveTourPartialState.GeofenceExited(intent.stopId))
            }

            LiveTourIntent.DismissContent -> {
                emit(LiveTourPartialState.ContentDismissed)
            }

            LiveTourIntent.DismissRouteDeviation -> {
                emit(LiveTourPartialState.RouteDeviationDismissed)
            }

            is LiveTourIntent.PlayAudio -> {
                handleAudioPlayback(intent.audioUrl)
            }

            LiveTourIntent.PauseAudio -> audioPlayerManager.pause()
            LiveTourIntent.ResumeAudio -> audioPlayerManager.play()
            LiveTourIntent.StopAudio -> audioPlayerManager.stop()

            is LiveTourIntent.SeekAudio -> {
                audioPlayerManager.seekTo(intent.position)
            }
        }
    }

    override fun reduceUiState(
        previousState: LiveTourUiState,
        partialState: LiveTourPartialState
    ): LiveTourUiState {
        return when (partialState) {
            LiveTourPartialState.Loading -> previousState.copy(
                isLoading = true,
                error = null
            )

            is LiveTourPartialState.TourDataLoaded -> previousState.copy(
                tourTitle = partialState.title,
                tourStops = partialState.stops,
                isLoading = false,
                error = null
            )

            LiveTourPartialState.LocationTrackingStarted -> previousState.copy(
                isLocationEnabled = true,
                tourStatus = TourStatus.ACTIVE
            )

            is LiveTourPartialState.LocationUpdated -> {
                val updatedState = previousState.copy(userLocation = partialState.location)

                // Check for route deviation
                val deviation = checkRouteDeviation(partialState.location, updatedState.tourStops)
                if (deviation.isDeviated && !updatedState.showRouteDeviationWarning) {
                    updatedState.copy(
                        routeDeviation = deviation,
                        showRouteDeviationWarning = true
                    )
                } else {
                    updatedState
                }
            }

            LiveTourPartialState.TourPaused -> previousState.copy(
                tourStatus = TourStatus.PAUSED
            )

            LiveTourPartialState.TourResumed -> previousState.copy(
                tourStatus = TourStatus.ACTIVE
            )

            LiveTourPartialState.TourCompleted -> previousState.copy(
                tourStatus = TourStatus.COMPLETED,
                progress = 1.0f
            )

            is LiveTourPartialState.GeofenceEntered -> {
                handleGeofenceEnteredState(previousState, partialState.stopId)
            }

            is LiveTourPartialState.GeofenceExited -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    if (stop.id == partialState.stopId) {
                        stop.copy(isActive = false)
                    } else stop
                }
                previousState.copy(
                    tourStops = updatedStops,
                    currentStop = null
                )
            }

            LiveTourPartialState.ContentDismissed -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    stop.copy(isActive = false)
                }
                previousState.copy(
                    tourStops = updatedStops,
                    currentStop = null
                )
            }

            is LiveTourPartialState.RouteDeviationDetected -> previousState.copy(
                routeDeviation = partialState.deviation,
                showRouteDeviationWarning = true
            )

            LiveTourPartialState.RouteDeviationDismissed -> previousState.copy(
                showRouteDeviationWarning = false
            )

            is LiveTourPartialState.AudioPlayerStateChanged -> previousState.copy(
                audioPlayerState = partialState.state
            )

            is LiveTourPartialState.AudioStarted -> previousState.copy(
                currentlyPlayingAudio = partialState.audioUrl
            )

            is LiveTourPartialState.LocationPermissionDenied -> previousState.copy(
                isLoading = false,
                error = "Location permission is required for live tours"
            )

            is LiveTourPartialState.LocationServiceError -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )

            is LiveTourPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private suspend fun handleStartLocationTracking() {
        try {
            if (!locationManager.hasLocationPermission()) {
                emit(LiveTourPartialState.LocationPermissionDenied)
                return
            }

            locationManager.startLocationUpdates()
            setupGeofencing()
            emit(LiveTourPartialState.LocationTrackingStarted)

        } catch (e: Exception) {
            Timber.e(e, "Failed to start location tracking")
            emit(LiveTourPartialState.LocationServiceError("Failed to start location tracking"))
        }
    }

    private suspend fun handleTourCompletion() {
        val state = uiStateSnapshot.value
        val completionPercentage =
            state.visitedStopsCount.toFloat() / state.tourStops.size.coerceAtLeast(1)

        tourAnalytics.endTourSession(
            tourId = tourId,
            completionPercentage = completionPercentage
        )

        locationManager.stopLocationUpdates()
        geofencingManager.removeAllGeofences()
        audioPlayerManager.release()

        emit(LiveTourPartialState.TourCompleted)
        publishEvent(LiveTourEvent.TourCompleted)
    }

    private suspend fun handleTourExit() {
        locationManager.stopLocationUpdates()
        geofencingManager.removeAllGeofences()
        audioPlayerManager.release()

        // Track incomplete tour
        val state = uiStateSnapshot.value
        val completionPercentage =
            state.visitedStopsCount.toFloat() / state.tourStops.size.coerceAtLeast(1)
        tourAnalytics.endTourSession(tourId, completionPercentage)
    }

    private suspend fun handleGeofenceEntered(stopId: String) {
        val currentStops = uiStateSnapshot.value.tourStops
        val stop = currentStops.find { it.id == stopId }

        if (stop != null) {
            tourAnalytics.trackStopVisited(
                tourId = tourId,
                stopId = stopId,
                stopName = stop.name
            )

            tourAnalytics.trackGeofenceEvent(
                tourId = tourId,
                stopId = stopId,
                eventType = "enter"
            )

            // Auto-play audio if available
            stop.content?.audioUrl?.let { audioUrl ->
                handleAudioPlayback(audioUrl)
            }
        }

        emit(LiveTourPartialState.GeofenceEntered(stopId))
    }

    private suspend fun handleAudioPlayback(audioUrl: String) {
        try {
            tourAnalytics.trackAudioPlayed(
                tourId = tourId,
                stopId = uiStateSnapshot.value.currentStop?.id ?: "",
                audioUrl = audioUrl,
                duration = 0L
            )

            audioPlayerManager.loadAudio(audioUrl)
            audioPlayerManager.play()
            emit(LiveTourPartialState.AudioStarted(audioUrl))

        } catch (e: Exception) {
            Timber.e(e, "Failed to play audio")
            emit(LiveTourPartialState.Error("Failed to play audio: ${e.message}"))
        }
    }

    private fun handleGeofenceEnteredState(
        previousState: LiveTourUiState,
        stopId: String
    ): LiveTourUiState {
        val updatedStops = previousState.tourStops.map { stop ->
            if (stop.id == stopId) {
                stop.copy(isActive = true, isVisited = true)
            } else {
                stop.copy(isActive = false)
            }
        }

        val stopIndex = updatedStops.indexOfFirst { it.id == stopId }
        val visitedCount = updatedStops.count { it.isVisited }
        val currentStop = updatedStops.find { it.id == stopId }

        return previousState.copy(
            tourStops = updatedStops,
            currentStopIndex = if (stopIndex >= 0) stopIndex else previousState.currentStopIndex,
            currentStop = currentStop,
            progress = visitedCount.toFloat() / updatedStops.size,
            visitedStopsCount = visitedCount
        )
    }

    private fun loadTourData(): Flow<LiveTourPartialState> = flow {
        emit(LiveTourPartialState.Loading)
        try {
            val tourDetail = mockDataManager.getTourDetail(tourId)
            if (tourDetail != null) {
                val liveStops = tourDetail.stops.mapIndexed { index, stop ->
                    LiveTourStop(
                        id = stop.id,
                        name = stop.name,
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        order = index + 1,
                        geofenceRadius = 50f,
                        content = StopContent(
                            text = stop.description,
                            imageUrls = emptyList(), // Add when available in mock data
                            audioUrl = generateMockAudioUrl(stop.id) // Mock audio URLs
                        )
                    )
                }
                emit(LiveTourPartialState.TourDataLoaded(tourDetail.title, liveStops))
            } else {
                emit(LiveTourPartialState.Error("Tour not found"))
            }
        } catch (e: Exception) {
            emit(LiveTourPartialState.Error("Failed to load tour data: ${e.message}"))
        }
    }

    private fun observeLocationUpdates(): Flow<LiveTourPartialState> {
        return locationManager.locationUpdates.map { location ->
            LiveTourPartialState.LocationUpdated(
                UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
            )
        }
    }

    private fun observeGeofenceEvents(): Flow<LiveTourPartialState> = flow {
        geofencingManager.geofenceEvents.collect { event ->
            when (event) {
                is GeofenceEvent.Enter -> {
                    emit(LiveTourPartialState.GeofenceEntered(event.geofenceId))
                }

                is GeofenceEvent.Exit -> {
                    emit(LiveTourPartialState.GeofenceExited(event.geofenceId))
                }
            }
        }
    }

    private fun observeAudioPlayerState(): Flow<LiveTourPartialState> = flow {
        combine(
            audioPlayerManager.playbackState,
            audioPlayerManager.currentPosition,
            audioPlayerManager.duration
        ) { playbackState, position, duration ->
            AudioPlayerState(playbackState, position, duration)
        }.collect { playerState ->
            emit(LiveTourPartialState.AudioPlayerStateChanged(playerState))
        }
    }

    private fun setupGeofencing() {
        val stops = uiStateSnapshot.value.tourStops
        if (stops.isNotEmpty()) {
            val geofences = stops.map { stop ->
                com.xwurfel.tourry.feature.geofencing.TourStopGeofence(
                    id = stop.id,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    radius = stop.geofenceRadius
                )
            }

            val result = geofencingManager.addGeofencesForTour(geofences)
            if (result.isFailure) {
                Timber.e("Failed to setup geofencing: ${result.exceptionOrNull()}")
            }
        }
    }

    private fun checkRouteDeviation(
        userLocation: UserLocation,
        tourStops: List<LiveTourStop>
    ): RouteDeviation {
        if (tourStops.isEmpty()) {
            return RouteDeviation(false, 0.0, null)
        }

        val visitedCount = uiStateSnapshot.value.visitedStopsCount
        val nextStops = tourStops.drop(visitedCount).take(2)

        if (nextStops.isEmpty()) {
            return RouteDeviation(false, 0.0, null)
        }

        val nearestStop = nextStops.minByOrNull { stop ->
            SphericalUtil.computeDistanceBetween(
                LatLng(userLocation.latitude, userLocation.longitude),
                LatLng(stop.latitude, stop.longitude)
            )
        }

        val distanceToRoute = nearestStop?.let { stop ->
            SphericalUtil.computeDistanceBetween(
                LatLng(userLocation.latitude, userLocation.longitude),
                LatLng(stop.latitude, stop.longitude)
            )
        } ?: Double.MAX_VALUE

        val isDeviated = distanceToRoute > MAX_DEVIATION_DISTANCE_METERS

        return RouteDeviation(isDeviated, distanceToRoute, nearestStop?.name)
    }

    private fun generateMockAudioUrl(stopId: String): String {
        // Generate mock audio URLs for demonstration
        return "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav"
    }

    override fun onCleared() {
        super.onCleared()

        val state = uiStateSnapshot.value
        if (state.tourStatus != TourStatus.COMPLETED) {
            val completionPercentage = state.visitedStopsCount.toFloat() /
                    state.tourStops.size.coerceAtLeast(1)
            tourAnalytics.endTourSession(tourId, completionPercentage)
        }

        locationManager.stopLocationUpdates()
        audioPlayerManager.release()
        geofencingManager.removeAllGeofences()
    }

    companion object {
        private const val MAX_DEVIATION_DISTANCE_METERS = 200.0
    }
}

// UI State
data class LiveTourUiState(
    val tourTitle: String = "",
    val tourStops: List<LiveTourStop> = emptyList(),
    val currentStopIndex: Int = 0,
    val currentStop: LiveTourStop? = null,
    val userLocation: UserLocation? = null,
    val tourStatus: TourStatus = TourStatus.PREPARING,
    val progress: Float = 0f,
    val visitedStopsCount: Int = 0,
    val isLocationEnabled: Boolean = false,
    val routeDeviation: RouteDeviation? = null,
    val showRouteDeviationWarning: Boolean = false,
    val audioPlayerState: AudioPlayerState? = null,
    val currentlyPlayingAudio: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canComplete: Boolean
        get() = visitedStopsCount == tourStops.size && tourStops.isNotEmpty()

    val nextStop: LiveTourStop?
        get() = tourStops.getOrNull(currentStopIndex + 1)

    val completionPercentage: Float
        get() = if (tourStops.isEmpty()) 0f else visitedStopsCount.toFloat() / tourStops.size
}

// Partial States
sealed interface LiveTourPartialState {
    object Loading : LiveTourPartialState

    data class TourDataLoaded(
        val title: String,
        val stops: List<LiveTourStop>
    ) : LiveTourPartialState

    object LocationTrackingStarted : LiveTourPartialState
    object LocationPermissionDenied : LiveTourPartialState
    data class LocationServiceError(val message: String) : LiveTourPartialState

    data class LocationUpdated(val location: UserLocation) : LiveTourPartialState

    object TourPaused : LiveTourPartialState
    object TourResumed : LiveTourPartialState
    object TourCompleted : LiveTourPartialState

    data class GeofenceEntered(val stopId: String) : LiveTourPartialState
    data class GeofenceExited(val stopId: String) : LiveTourPartialState

    object ContentDismissed : LiveTourPartialState

    data class RouteDeviationDetected(val deviation: RouteDeviation) : LiveTourPartialState
    object RouteDeviationDismissed : LiveTourPartialState

    data class AudioPlayerStateChanged(val state: AudioPlayerState) : LiveTourPartialState
    data class AudioStarted(val audioUrl: String) : LiveTourPartialState

    data class Error(val message: String) : LiveTourPartialState
}

// Intents
sealed interface LiveTourIntent {
    object StartLocationTracking : LiveTourIntent
    object PauseTour : LiveTourIntent
    object ResumeTour : LiveTourIntent
    object CompleteTour : LiveTourIntent
    object ExitTour : LiveTourIntent

    object DismissContent : LiveTourIntent
    object DismissRouteDeviation : LiveTourIntent

    data class PlayAudio(val audioUrl: String) : LiveTourIntent
    object PauseAudio : LiveTourIntent
    object ResumeAudio : LiveTourIntent
    object StopAudio : LiveTourIntent
    data class SeekAudio(val position: Int) : LiveTourIntent

    data class OnGeofenceEntered(val stopId: String) : LiveTourIntent
    data class OnGeofenceExited(val stopId: String) : LiveTourIntent
}

// Events
sealed interface LiveTourEvent {
    object TourCompleted : LiveTourEvent
    object NavigateBack : LiveTourEvent
}

// Data Models
data class LiveTourStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val geofenceRadius: Float = 50f, // meters
    val isActive: Boolean = false,
    val isVisited: Boolean = false,
    val content: StopContent? = null
) {
    val isCompleted: Boolean get() = isVisited
    val isNextStop: Boolean get() = !isVisited && !isActive
}


data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isAccurate: Boolean get() = accuracy <= 20f // Within 20 meters

    fun distanceTo(latitude: Double, longitude: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            this.latitude, this.longitude,
            latitude, longitude,
            results
        )
        return results[0]
    }
}

data class RouteDeviation(
    val isDeviated: Boolean,
    val distanceFromRoute: Double,
    val nearestStopName: String?
) {
    val severityLevel: DeviationSeverity
        get() = when {
            !isDeviated -> DeviationSeverity.NONE
            distanceFromRoute < 100 -> DeviationSeverity.MINOR
            distanceFromRoute < 300 -> DeviationSeverity.MODERATE
            else -> DeviationSeverity.MAJOR
        }
}

enum class DeviationSeverity {
    NONE, MINOR, MODERATE, MAJOR
}

enum class TourStatus {
    PREPARING,  // Initial state, waiting for location permission/setup
    ACTIVE,     // Tour is running, tracking location
    PAUSED,     // User paused the tour
    COMPLETED   // Tour finished successfully
}

data class AudioPlayerState(
    val playbackState: PlaybackState,
    val currentPosition: Int,
    val duration: Int
) {
    val isPlaying: Boolean get() = playbackState == PlaybackState.PLAYING
    val isPaused: Boolean get() = playbackState == PlaybackState.PAUSED
    val isLoading: Boolean get() = playbackState == PlaybackState.LOADING
    val hasError: Boolean get() = playbackState == PlaybackState.ERROR

    val progressPercentage: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
}