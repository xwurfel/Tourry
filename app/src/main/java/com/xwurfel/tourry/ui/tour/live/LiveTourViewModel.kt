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
import com.xwurfel.tourry.feature.geofencing.TourStopGeofence
import com.xwurfel.tourry.feature.location.service.LocationService
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
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
            observeGeofenceEvents(),
            simulateLocationUpdates(),
            observeLocationForRouteDeviation(),
            observeAudioPlayerState()
        )
    }

    override fun mapIntents(intent: LiveTourIntent): Flow<LiveTourPartialState> = flow {
        when (intent) {
            LiveTourIntent.StartLocationTracking -> {
                emit(LiveTourPartialState.LocationTrackingStarted)
                startLocationService()
                setupGeofencing()
            }

            is LiveTourIntent.DismissRouteDeviation -> {
                emit(LiveTourPartialState.RouteDeviationDismissed)
            }

            LiveTourIntent.PauseTour -> {
                tourAnalytics.trackTourPaused(tourId, "user_action")
                emit(LiveTourPartialState.TourPaused)
                stopLocationService()
            }

            LiveTourIntent.ResumeTour -> {
                emit(LiveTourPartialState.TourResumed)
                startLocationService()
            }

            LiveTourIntent.CompleteTour -> {
                val state = uiStateSnapshot.value
                val completionPercentage = state.visitedStopsCount.toFloat() / state.tourStops.size

                // Track tour completion
                tourAnalytics.endTourSession(
                    tourId = tourId,
                    completionPercentage = completionPercentage
                )

                emit(LiveTourPartialState.TourCompleted)
                stopLocationService()
                geofencingManager.removeAllGeofences()
                publishEvent(LiveTourEvent.TourCompleted)
            }


            LiveTourIntent.ExitTour -> {
                emit(LiveTourPartialState.TourExited)
                stopLocationService()
                geofencingManager.removeAllGeofences()
                publishEvent(LiveTourEvent.NavigateBack)
            }

            LiveTourIntent.DismissContent -> {
                emit(LiveTourPartialState.ContentDismissed)
            }

            is LiveTourIntent.OnGeofenceEntered -> {
                tourAnalytics.trackStopVisited(
                    tourId = tourId,
                    stopId = intent.stopId,
                    stopName = uiStateSnapshot.value.tourStops.find { it.id == intent.stopId }?.name
                        ?: ""
                )

                tourAnalytics.trackGeofenceEvent(
                    tourId = tourId,
                    stopId = intent.stopId,
                    eventType = "enter"
                )

                emit(LiveTourPartialState.GeofenceEntered(intent.stopId))
            }

            is LiveTourIntent.OnGeofenceExited -> {
                emit(LiveTourPartialState.GeofenceExited(intent.stopId))
            }

            is LiveTourIntent.LocationUpdated -> {
                emit(LiveTourPartialState.LocationUpdated(intent.location))
            }

            is LiveTourIntent.SimulateStopVisit -> {
                // For testing purposes - simulate visiting a stop
                emit(LiveTourPartialState.GeofenceEntered(intent.stopId))
            }

            is LiveTourIntent.PlayAudio -> {
                tourAnalytics.trackAudioPlayed(
                    tourId = tourId,
                    stopId = uiStateSnapshot.value.currentStop?.id ?: "",
                    audioUrl = intent.audioUrl,
                    duration = 0L
                )

                audioPlayerManager.loadAudio(intent.audioUrl)
                audioPlayerManager.play()
                emit(LiveTourPartialState.AudioStarted(intent.audioUrl))
            }

            LiveTourIntent.PauseAudio -> {
                audioPlayerManager.pause()
            }

            LiveTourIntent.ResumeAudio -> {
                audioPlayerManager.play()
            }

            LiveTourIntent.StopAudio -> {
                audioPlayerManager.stop()
            }

            is LiveTourIntent.SeekAudio -> {
                audioPlayerManager.seekTo(intent.position)
            }
        }
    }

    override fun reduceUiState(
        previousState: LiveTourUiState, partialState: LiveTourPartialState
    ): LiveTourUiState {
        return when (partialState) {
            LiveTourPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is LiveTourPartialState.TourDataLoaded -> previousState.copy(
                tourTitle = partialState.title,
                tourStops = partialState.stops,
                isLoading = false,
                error = null
            )

            LiveTourPartialState.LocationTrackingStarted -> previousState.copy(
                isLocationEnabled = true
            )

            is LiveTourPartialState.LocationUpdated -> previousState.copy(
                userLocation = partialState.location
            )

            LiveTourPartialState.TourPaused -> previousState.copy(
                tourStatus = TourStatus.PAUSED
            )

            LiveTourPartialState.TourResumed -> previousState.copy(
                tourStatus = TourStatus.ACTIVE
            )

            LiveTourPartialState.TourCompleted -> previousState.copy(
                tourStatus = TourStatus.COMPLETED, progress = 1.0f
            )

            is LiveTourPartialState.RouteDeviationDetected -> {
                previousState.copy(
                    routeDeviation = partialState.deviation, showRouteDeviationWarning = true
                )
            }

            LiveTourPartialState.RouteDeviationDismissed -> {
                previousState.copy(
                    showRouteDeviationWarning = false
                )
            }

            LiveTourPartialState.TourExited -> previousState

            is LiveTourPartialState.GeofenceEntered -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    if (stop.id == partialState.stopId) {
                        stop.copy(isActive = true, isVisited = true)
                    } else {
                        stop.copy(isActive = false)
                    }
                }
                val stopIndex = updatedStops.indexOfFirst { it.id == partialState.stopId }
                val visitedCount = updatedStops.count { it.isVisited }

                previousState.copy(
                    tourStops = updatedStops,
                    currentStopIndex = if (stopIndex >= 0) stopIndex else previousState.currentStopIndex,
                    currentStop = updatedStops.find { it.id == partialState.stopId },
                    progress = visitedCount.toFloat() / updatedStops.size,
                    visitedStopsCount = visitedCount
                )
            }

            is LiveTourPartialState.GeofenceExited -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    if (stop.id == partialState.stopId) {
                        stop.copy(isActive = false)
                    } else stop
                }
                previousState.copy(
                    tourStops = updatedStops, currentStop = null
                )
            }

            LiveTourPartialState.ContentDismissed -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    stop.copy(isActive = false)
                }
                previousState.copy(
                    tourStops = updatedStops, currentStop = null
                )
            }

            is LiveTourPartialState.AudioPlayerStateChanged -> {
                previousState.copy(audioPlayerState = partialState.state)
            }

            is LiveTourPartialState.AudioStarted -> {
                previousState.copy(currentlyPlayingAudio = partialState.audioUrl)
            }

            is LiveTourPartialState.Error -> previousState.copy(
                isLoading = false, error = partialState.message
            )
        }
    }

    private fun loadTourData(): Flow<LiveTourPartialState> = flow {
        emit(LiveTourPartialState.Loading)
        try {
            val tourDetail = mockDataManager.getTourDetail(tourId)
            if (tourDetail != null) {
                val liveStops = tourDetail.stops.map { stop ->
                    LiveTourStop(
                        id = stop.id,
                        name = stop.name,
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        geofenceRadius = 50f,
                        content = StopContent(
                            text = stop.description,
                            imageUrl = null, // Mock data doesn't have images yet
                            audioUrl = null  // Mock data doesn't have audio yet
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

    private fun observeLocationForRouteDeviation(): Flow<LiveTourPartialState> = flow {
        combine(
            flow { emit(uiStateSnapshot.value.userLocation) },
            flow { emit(uiStateSnapshot.value.tourStops) }) { location, stops ->
            location to stops
        }.collect { (location, stops) ->
            if (location != null && stops.isNotEmpty()) {
                val deviation = checkRouteDeviation(location, stops)
                if (deviation.isDeviated) {
                    emit(LiveTourPartialState.RouteDeviationDetected(deviation))
                }
            }
        }
    }

    private fun checkRouteDeviation(
        userLocation: UserLocation, tourStops: List<LiveTourStop>
    ): RouteDeviation {
        val currentStopIndex = uiStateSnapshot.value.currentStopIndex
        val visitedStopsCount = uiStateSnapshot.value.visitedStopsCount

        val relevantStops = tourStops.drop(visitedStopsCount).take(2)

        if (relevantStops.isEmpty()) {
            return RouteDeviation(false, 0.0, null)
        }

        val nearestStop = relevantStops.minByOrNull { stop ->
            calculateDistance(
                userLocation.latitude, userLocation.longitude, stop.latitude, stop.longitude
            )
        }

        val distanceToRoute = nearestStop?.let { stop ->
            calculateDistance(
                userLocation.latitude, userLocation.longitude, stop.latitude, stop.longitude
            )
        } ?: Double.MAX_VALUE

        val isDeviated = distanceToRoute > MAX_DEVIATION_DISTANCE

        return RouteDeviation(isDeviated, distanceToRoute, nearestStop?.name)
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return SphericalUtil.computeDistanceBetween(
            LatLng(lat1, lon1), LatLng(lat2, lon2)
        )
    }

    // Mock location updates for testing
    private fun simulateLocationUpdates(): Flow<LiveTourPartialState> = flow {
        kotlinx.coroutines.delay(2000) // Wait for tour data to load

        val stops = uiStateSnapshot.value.tourStops
        if (stops.isNotEmpty()) {
            // Start near the first stop
            val firstStop = stops.first()
            emit(
                LiveTourPartialState.LocationUpdated(
                    UserLocation(
                        latitude = firstStop.latitude + 0.0001, // Slightly offset
                        longitude = firstStop.longitude + 0.0001, accuracy = 10f
                    )
                )
            )

            // Simulate moving through stops every 30 seconds for demo
            kotlinx.coroutines.delay(5000)
            stops.forEach { stop ->
                emit(
                    LiveTourPartialState.LocationUpdated(
                        UserLocation(
                            latitude = stop.latitude, longitude = stop.longitude, accuracy = 5f
                        )
                    )
                )

                // Simulate entering the geofence
                kotlinx.coroutines.delay(1000)
                emit(LiveTourPartialState.GeofenceEntered(stop.id))

                // Stay at stop for a bit
                kotlinx.coroutines.delay(10000)

                // Exit geofence
                emit(LiveTourPartialState.GeofenceExited(stop.id))

                // Move to next stop
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    private fun startLocationService() {
        try {
            val intent = LocationService.getStartIntent(
                context,
                tourId,
                uiStateSnapshot.value.tourTitle
            )
            context.startForegroundService(intent)
        } catch (e: SecurityException) {
            // Handle permission issues
            Timber.e(e, "Failed to start location service due to permissions")
            // Could emit an error state here if needed
        } catch (e: Exception) {
            Timber.e(e, "Failed to start location service")
            // Handle other service start failures
        }
    }

    private fun stopLocationService() {
        try {
            val intent = LocationService.getStopIntent(context)
            context.stopService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop location service")
        }
    }

    private fun setupGeofencing() {
        val stops = uiStateSnapshot.value.tourStops
        if (stops.isNotEmpty()) {
            val geofences = stops.map { stop ->
                TourStopGeofence(
                    id = stop.id,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    radius = stop.geofenceRadius
                )
            }
            geofencingManager.addGeofencesForTour(geofences)
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

    override fun onCleared() {
        super.onCleared()

        val state = uiStateSnapshot.value
        if (state.tourStatus != TourStatus.COMPLETED) {
            val completionPercentage =
                state.visitedStopsCount.toFloat() / maxOf(state.tourStops.size, 1)
            tourAnalytics.endTourSession(tourId, completionPercentage)
        }

        stopLocationService()
        audioPlayerManager.release()
        geofencingManager.removeAllGeofences()
    }

    companion object {
        private const val MAX_DEVIATION_DISTANCE = 200.0
    }
}

// States
data class LiveTourUiState(
    val tourTitle: String = "",
    val tourStops: List<LiveTourStop> = emptyList(),
    val currentStopIndex: Int = 0,
    val currentStop: LiveTourStop? = null,
    val userLocation: UserLocation? = null,
    val tourStatus: TourStatus = TourStatus.ACTIVE,
    val progress: Float = 0f,
    val visitedStopsCount: Int = 0,
    val isLocationEnabled: Boolean = false,
    val routeDeviation: RouteDeviation? = null,
    val showRouteDeviationWarning: Boolean = false,
    val audioPlayerState: AudioPlayerState? = null,
    val currentlyPlayingAudio: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface LiveTourPartialState {
    object Loading : LiveTourPartialState
    data class TourDataLoaded(
        val title: String, val stops: List<LiveTourStop>
    ) : LiveTourPartialState

    data class AudioPlayerStateChanged(val state: AudioPlayerState) : LiveTourPartialState
    data class AudioStarted(val audioUrl: String) : LiveTourPartialState

    object LocationTrackingStarted : LiveTourPartialState
    data class LocationUpdated(val location: UserLocation) : LiveTourPartialState
    object TourPaused : LiveTourPartialState
    object TourResumed : LiveTourPartialState
    object TourCompleted : LiveTourPartialState
    object TourExited : LiveTourPartialState
    data class GeofenceEntered(val stopId: String) : LiveTourPartialState
    data class GeofenceExited(val stopId: String) : LiveTourPartialState
    object ContentDismissed : LiveTourPartialState

    data class RouteDeviationDetected(val deviation: RouteDeviation) : LiveTourPartialState
    object RouteDeviationDismissed : LiveTourPartialState

    data class Error(val message: String) : LiveTourPartialState
}

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
    data class LocationUpdated(val location: UserLocation) : LiveTourIntent
    data class SimulateStopVisit(val stopId: String) : LiveTourIntent // For testing
}

sealed interface LiveTourEvent {
    object TourCompleted : LiveTourEvent
    object NavigateBack : LiveTourEvent
}

// Data models
data class LiveTourStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadius: Float = 50f, // meters
    val isActive: Boolean = false,
    val isVisited: Boolean = false,
    val content: StopContent? = null
)

data class StopContent(
    val text: String, val imageUrl: String? = null, val audioUrl: String? = null
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class RouteDeviation(
    val isDeviated: Boolean, val distanceFromRoute: Double, val nearestStopName: String?
)

enum class TourStatus {
    ACTIVE, PAUSED, COMPLETED
}

data class AudioPlayerState(
    val playbackState: PlaybackState,
    val currentPosition: Int,
    val duration: Int
)