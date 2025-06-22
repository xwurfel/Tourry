package com.xwurfel.tourry.ui.tour.live

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.audio.AudioPlayerManager
import com.xwurfel.tourry.feature.audio.domain.model.AudioPlayerState
import com.xwurfel.tourry.feature.geofencing.GeofenceEvent
import com.xwurfel.tourry.feature.geofencing.GeofencingManager
import com.xwurfel.tourry.feature.location.LocationManager
import com.xwurfel.tourry.feature.location.domain.model.UserLocation
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import com.xwurfel.tourry.feature.tours.domain.model.RouteDeviation
import com.xwurfel.tourry.feature.tours.domain.model.TourStatus
import com.xwurfel.tourry.feature.tours.domain.usecase.CompleteTourSessionUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.GetLiveTourUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.RecordStopVisitUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.StartTourSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationManager: LocationManager,
    private val geofencingManager: GeofencingManager,
    private val audioPlayerManager: AudioPlayerManager,
    private val tourAnalytics: TourAnalytics,
    private val getLiveTourUseCase: GetLiveTourUseCase,
    private val startTourSessionUseCase: StartTourSessionUseCase,
    private val recordStopVisitUseCase: RecordStopVisitUseCase,
    private val completeTourSessionUseCase: CompleteTourSessionUseCase,
    @ApplicationContext private val context: Context,
) : MviViewModel<LiveTourUiState, LiveTourPartialState, LiveTourEvent, LiveTourIntent>(
    initialState = LiveTourUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""
    private var currentSessionId: String? = null
    private var tourStartTime: Long = 0L

    init {
        observeContinuousChanges(
            loadTourData(),
            observeLocationUpdates(),
            observeGeofenceEvents(),
            observeAudioPlayerState(),
            observeServiceConnection()
        )
    }

    override fun mapIntents(intent: LiveTourIntent): Flow<LiveTourPartialState> = flow {
        when (intent) {
            LiveTourIntent.StartLocationTracking -> {
                try {
                    if (!locationManager.hasLocationPermission()) {
                        emit(LiveTourPartialState.LocationPermissionDenied)
                        return@flow
                    }

                    emit(LiveTourPartialState.Loading)

                    val sessionResult = startTourSessionUseCase(tourId, getCurrentUserId())
                    sessionResult.onSuccess { sessionId ->
                        currentSessionId = sessionId
                        tourStartTime = System.currentTimeMillis()
                    }.onFailure { error ->
                        emit(
                            LiveTourPartialState.Error(
                                "Failed to start tour session: ${
                                    error.msg.asString(
                                        context.resources
                                    )
                                }"
                            )
                        )
                        return@flow
                    }

                    val result =
                        locationManager.startLocationUpdates(
                            tourId,
                            uiStateSnapshot.value.tourTitle
                        )

                    if (result.isSuccess) {
                        setupGeofencing()
                        tourAnalytics.startTourSession(
                            tourId,
                            uiStateSnapshot.value.tourTitle,
                            false
                        )
                        LiveTourPartialState.LocationTrackingStarted
                    } else {
                        val error = result.exceptionOrNull()
                        Timber.e(error, "Failed to start location tracking")
                        LiveTourPartialState.LocationServiceError(
                            error?.message ?: "Failed to start location tracking"
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start location tracking")
                    LiveTourPartialState.LocationServiceError("Failed to start location tracking")
                }
            }

            LiveTourIntent.PauseTour -> {
                tourAnalytics.trackTourPaused(tourId, "user_action")
                locationManager.stopLocationUpdates()
                emit(LiveTourPartialState.TourPaused)
            }

            LiveTourIntent.ResumeTour -> {
                locationManager.startLocationUpdates()
                tourAnalytics.trackTourResumed(tourId)
                emit(LiveTourPartialState.TourResumed)
            }

            LiveTourIntent.CompleteTour -> {
                emit(completeTour())
            }

            LiveTourIntent.ExitTour -> {
                handleTourExit()
                publishEvent(LiveTourEvent.NavigateBack)
            }

            is LiveTourIntent.OnGeofenceEntered -> {
                emit(handleGeofenceEntered(intent.stopId))
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
                checkAndHandleRouteDeviation(updatedState, partialState.location)
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

    private suspend fun completeTour(): LiveTourPartialState {
        return try {
            val state = uiStateSnapshot.value
            val completionPercentage =
                state.visitedStopsCount.toFloat() / state.tourStops.size.coerceAtLeast(1)
            val totalDuration = System.currentTimeMillis() - tourStartTime

            currentSessionId?.let { sessionId ->
                completeTourSessionUseCase(sessionId, completionPercentage, totalDuration)
                    .onFailure { error ->
                        Timber.e(
                            "Failed to complete tour session: ${
                                error.msg.asString(
                                    context.resources
                                )
                            }"
                        )
                    }
            }

            tourAnalytics.endTourSession(tourId, completionPercentage)
            cleanupResources()

            // Trigger navigation to summary
            publishEvent(LiveTourEvent.TourCompleted)
            LiveTourPartialState.TourCompleted
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete tour")
            LiveTourPartialState.Error("Failed to complete tour: ${e.message}")
        }
    }

    private suspend fun handleGeofenceEntered(stopId: String): LiveTourPartialState {
        val currentStops = uiStateSnapshot.value.tourStops
        val stop = currentStops.find { it.id == stopId }

        if (stop != null) {
            // Record stop visit
            currentSessionId?.let { sessionId ->
                val userLocation = uiStateSnapshot.value.userLocation
                userLocation?.let { location ->
                    recordStopVisitUseCase(
                        sessionId = sessionId,
                        stopId = stopId,
                        timestamp = System.currentTimeMillis(),
                        userLocation = Pair(location.latitude, location.longitude)
                    ).onFailure { error ->
                        Timber.e(
                            "Failed to record stop visit: ${
                                error.msg.asString(
                                    context.resources
                                )
                            }"
                        )
                    }
                }
            }

            tourAnalytics.trackStopVisited(tourId, stopId, stop.name)
            tourAnalytics.trackGeofenceEvent(tourId, stopId, "enter")

            // Auto-play audio if available
            stop.content?.audioUrl?.let { audioUrl ->
                handleAudioPlayback(audioUrl)
            }
        }

        return LiveTourPartialState.GeofenceEntered(stopId)
    }

    private fun handleTourExit() {
        cleanupResources()
        val state = uiStateSnapshot.value
        val completionPercentage =
            state.visitedStopsCount.toFloat() / state.tourStops.size.coerceAtLeast(1)
        tourAnalytics.endTourSession(tourId, completionPercentage)
    }

    private fun cleanupResources() {
        locationManager.stopLocationUpdates()
        geofencingManager.removeAllGeofences()
        audioPlayerManager.release()
    }

    private fun handleAudioPlayback(audioUrl: String) {
        try {
            tourAnalytics.trackAudioPlayed(
                tourId = tourId,
                stopId = uiStateSnapshot.value.currentStop?.id ?: "",
                audioUrl = audioUrl,
                duration = 0L
            )

            audioPlayerManager.loadAudio(audioUrl)
            audioPlayerManager.play()
        } catch (e: Exception) {
            Timber.e(e, "Failed to play audio")
        }
    }

    private fun checkAndHandleRouteDeviation(
        updatedState: LiveTourUiState,
        location: UserLocation
    ): LiveTourUiState {
        val deviation = checkRouteDeviation(location, updatedState.tourStops)
        return if (deviation.isDeviated && !updatedState.showRouteDeviationWarning) {
            updatedState.copy(
                routeDeviation = deviation,
                showRouteDeviationWarning = true
            )
        } else {
            updatedState
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

        getLiveTourUseCase(tourId)
            .onSuccess { liveTour ->
                emit(
                    LiveTourPartialState.TourDataLoaded(
                        title = liveTour.title,
                        stops = liveTour.stops.map { stop ->
                            LiveTourStop(
                                id = stop.id,
                                name = stop.name,
                                latitude = stop.latitude,
                                longitude = stop.longitude,
                                order = stop.order,
                                geofenceRadius = stop.geofenceRadius,
                                content = stop.content,
                                description = stop.description
                            )
                        }
                    ))
            }
            .onFailure { error ->
                emit(
                    LiveTourPartialState.Error(
                        "Failed to load tour: ${
                            error.msg.asString(
                                context.resources
                            )
                        }"
                    )
                )
            }
    }

    private fun observeLocationUpdates(): Flow<LiveTourPartialState> {
        return locationManager.locationUpdates
            .map<_, LiveTourPartialState> { location ->
                Timber.d(
                    "LiveTourViewModel received location update: " +
                            "${location.latitude}, ${location.longitude}"
                )
                LiveTourPartialState.LocationUpdated(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                )
            }
            .catch { error ->
                Timber.e(error, "Error in location updates flow")
                emit(
                    LiveTourPartialState.LocationServiceError(
                        "Location updates failed: ${error.message}"
                    )
                )
            }
    }

    private fun observeServiceConnection(): Flow<LiveTourPartialState> = flow {
        locationManager.isServiceConnected.collect { isConnected ->
            if (!isConnected && uiStateSnapshot.value.tourStatus == TourStatus.ACTIVE) {
                emit(LiveTourPartialState.LocationServiceError("Lost connection to location service"))
            }
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

    private fun getCurrentUserId(): String {
        // TODO: Get current user ID from authentication repository
        return "current_user_id"
    }

    override fun onCleared() {
        super.onCleared()
        cleanupResources()
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
    data object Loading : LiveTourPartialState

    data class TourDataLoaded(
        val title: String,
        val stops: List<LiveTourStop>
    ) : LiveTourPartialState

    data object LocationTrackingStarted : LiveTourPartialState
    data object LocationPermissionDenied : LiveTourPartialState
    data class LocationServiceError(val message: String) : LiveTourPartialState

    data class LocationUpdated(val location: UserLocation) : LiveTourPartialState

    data object TourPaused : LiveTourPartialState
    data object TourResumed : LiveTourPartialState
    data object TourCompleted : LiveTourPartialState

    data class GeofenceEntered(val stopId: String) : LiveTourPartialState
    data class GeofenceExited(val stopId: String) : LiveTourPartialState

    data object ContentDismissed : LiveTourPartialState

    data class RouteDeviationDetected(val deviation: RouteDeviation) : LiveTourPartialState
    data object RouteDeviationDismissed : LiveTourPartialState

    data class AudioPlayerStateChanged(val state: AudioPlayerState) : LiveTourPartialState
    data class AudioStarted(val audioUrl: String) : LiveTourPartialState

    data class Error(val message: String) : LiveTourPartialState
}

// Intents
sealed interface LiveTourIntent {
    data object StartLocationTracking : LiveTourIntent
    data object PauseTour : LiveTourIntent
    data object ResumeTour : LiveTourIntent
    data object CompleteTour : LiveTourIntent
    data object ExitTour : LiveTourIntent

    data object DismissContent : LiveTourIntent
    data object DismissRouteDeviation : LiveTourIntent

    data class PlayAudio(val audioUrl: String) : LiveTourIntent
    data object PauseAudio : LiveTourIntent
    data object ResumeAudio : LiveTourIntent
    data object StopAudio : LiveTourIntent
    data class SeekAudio(val position: Int) : LiveTourIntent

    data class OnGeofenceEntered(val stopId: String) : LiveTourIntent
    data class OnGeofenceExited(val stopId: String) : LiveTourIntent
}

// Events
sealed interface LiveTourEvent {
    data object TourCompleted : LiveTourEvent
    data object NavigateBack : LiveTourEvent
}


