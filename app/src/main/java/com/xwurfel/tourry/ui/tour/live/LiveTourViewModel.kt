package com.xwurfel.tourry.ui.tour.live

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.audio.AudioPlayerManager
import com.xwurfel.tourry.feature.audio.domain.model.AudioPlayerState
import com.xwurfel.tourry.feature.geofencing.DemoGeofencingManager
import com.xwurfel.tourry.feature.geofencing.GeofenceEvent
import com.xwurfel.tourry.feature.location.DemoLocationManager
import com.xwurfel.tourry.feature.location.domain.model.UserLocation
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import com.xwurfel.tourry.feature.tours.domain.model.RouteDeviation
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import com.xwurfel.tourry.feature.tours.domain.model.TourStatus
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourByIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.StartTourUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val locationManager: DemoLocationManager,
    private val geofencingManager: DemoGeofencingManager,
    private val demoTourCoordinator: DemoTourCoordinator,
    private val audioPlayerManager: AudioPlayerManager,
    private val getTourByIdUseCase: GetTourByIdUseCase,
    private val startTourUseCase: StartTourUseCase,
    @ApplicationContext private val context: Context,
) : MviViewModel<LiveTourUiState, LiveTourPartialState, LiveTourEvent, LiveTourIntent>(
    initialState = LiveTourUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""
    private var currentSessionId: String? = null
    private var tourStartTime: Long = 0L
    private var contentDismissJob: Job? = null

    init {
        observeContinuousChanges(
            loadTourData(),
            observeLocationUpdates(),
            observeGeofenceEvents(),
            observeAudioPlayerState(),
            observeServiceConnection(),
        )
    }

    private suspend fun handleDemoStart(): LiveTourPartialState {
        return try {
            Timber.d("🎭 Demo: Starting demo tour simulation")

            val tourStops = uiStateSnapshot.value.tourStops
            val tourTitle = uiStateSnapshot.value.tourTitle

            if (tourStops.isNotEmpty()) {
                Timber.d("🎭 Demo: Using actual tour data: $tourTitle with ${tourStops.size} stops")

                // Ensure all stops have some content if they don't already
                val stopsWithContent = tourStops.map { stop ->
                    val hasContent = stop.content != null && stop.content.text.isNotEmpty()
                    Timber.d("🎭 Demo: Stop ${stop.name} - hasContent: $hasContent")

                    if (!hasContent) {
                        val newContent = StopContent(
                            text = "📍 Welcome to ${stop.name}!\n\n${stop.description.ifEmpty { "Enjoy exploring this location on your tour." }}",
                            imageUrls = emptyList(),
                            audioUrl = null
                        )
                        Timber.d("🎭 Demo: Added content to ${stop.name}: ${newContent.text.take(50)}...")
                        stop.copy(content = newContent)
                    } else {
                        Timber.d(
                            "🎭 Demo: ${stop.name} already has content: ${
                                stop.content?.text?.take(
                                    50
                                )
                            }..."
                        )
                        stop
                    }
                }

                Timber.d("🎭 Demo: Final stop content check:")
                stopsWithContent.forEach { stop ->
                    Timber.d(
                        "  - ${stop.name}: hasContent=${stop.content != null}, text='${
                            stop.content?.text?.take(
                                30
                            )
                        }...'"
                    )
                }

                // Start demo simulation with actual tour data
                demoTourCoordinator.startTourSimulation(
                    tourId,
                    tourTitle,
                    stopsWithContent
                ).getOrThrow()

                Timber.d("✅ Demo: Tour simulation started successfully")
                LiveTourPartialState.TourDataUpdated(
                    title = tourTitle,
                    stops = stopsWithContent
                )
            } else {
                LiveTourPartialState.Error("No tour stops available for demo")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Demo: Failed to start demo tour")
            LiveTourPartialState.Error("Failed to start demo: ${e.message}")
        }
    }

    override fun mapIntents(intent: LiveTourIntent): Flow<LiveTourPartialState> = flow {
        when (intent) {
            LiveTourIntent.Start -> {
                emit(LiveTourPartialState.Loading)
                // Wait for tour data to load first
                delay(500L)
                emit(handleDemoStart())
                // Auto-start location tracking after demo setup
                delay(1000L)
                emit(LiveTourPartialState.AutoStartLocationTracking)
            }

            LiveTourIntent.StartLocationTracking -> {
                try {
                    if (!locationManager.hasLocationPermission()) {
                        emit(LiveTourPartialState.LocationPermissionDenied)
                        return@flow
                    }

                    emit(LiveTourPartialState.Loading)

                    val result = locationManager.startLocationUpdates(
                        tourId,
                        uiStateSnapshot.value.tourTitle
                    )

                    if (result.isSuccess) {
                        setupGeofencing()
                        emit(LiveTourPartialState.LocationTrackingStarted)
                    } else {
                        val error = result.exceptionOrNull()
                        Timber.e(error, "Failed to start location tracking")
                        emit(
                            LiveTourPartialState.LocationServiceError(
                                error?.message ?: "Failed to start location tracking"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start location tracking")
                    emit(LiveTourPartialState.LocationServiceError("Failed to start location tracking"))
                }
            }

            LiveTourIntent.ResumeTour -> {
                locationManager.startLocationUpdates()
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
                cancelContentDismissTimer()
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

            LiveTourIntent.AutoDismissContent -> {
                Timber.d("🕒 Demo: Auto-dismissing content after timer")
                emit(LiveTourPartialState.ContentDismissed)
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
                error = null,
                tourStatus = TourStatus.READY_TO_START
            )

            is LiveTourPartialState.TourDataUpdated -> previousState.copy(
                tourTitle = partialState.title,
                tourStops = partialState.stops,
                isLoading = false,
                error = null,
                tourStatus = TourStatus.READY_TO_START
            )

            LiveTourPartialState.AutoStartLocationTracking -> {
                // Auto-trigger location tracking
                viewModelScope.launch {
                    acceptIntent(LiveTourIntent.StartLocationTracking)
                }
                previousState
            }

            LiveTourPartialState.LocationTrackingStarted -> previousState.copy(
                isLocationEnabled = true,
                isLoading = false,
                tourStatus = TourStatus.ACTIVE // This is key - change to ACTIVE when location starts
            )

            is LiveTourPartialState.LocationUpdated -> {
                val updatedState = previousState.copy(userLocation = partialState.location)
                checkAndHandleRouteDeviation(updatedState, partialState.location)
            }

            LiveTourPartialState.TourResumed -> previousState.copy(
                tourStatus = TourStatus.ACTIVE
            )

            LiveTourPartialState.TourCompleted -> previousState.copy(
                tourStatus = TourStatus.COMPLETED,
                progress = 1.0f
            )

            is LiveTourPartialState.GeofenceEntered -> {
                Timber.d("🎯 Demo: Processing GeofenceEntered for stop: ${partialState.stopId}")
                val updatedState = handleGeofenceEnteredState(previousState, partialState.stopId)

                // Start auto-dismiss timer if content is present
                updatedState.currentStop?.let { stop ->
                    if (stop.isActive && stop.content != null) {
                        Timber.d("🎯 Demo: Starting auto-dismiss timer for stop: ${stop.name}")
                        Timber.d("🎯 Demo: Content available: ${stop.content.text.take(50)}...")
                        startContentDismissTimer()
                    } else {
                        Timber.w("⚠️ Demo: Stop not active or no content - isActive: ${stop.isActive}, hasContent: ${stop.content != null}")
                    }
                } ?: Timber.w("⚠️ Demo: No current stop set after geofence entry")

                // Check if tour should be completed
                val finalState = checkTourCompletion(updatedState)
                finalState
            }

            is LiveTourPartialState.GeofenceExited -> {
                cancelContentDismissTimer()
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
                cancelContentDismissTimer()
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

            is LiveTourPartialState.TourStatusChanged ->
                previousState.copy(tourStatus = partialState.newStatus)
        }
    }

    private fun completeTour(): LiveTourPartialState {
        return try {
            cleanupResources()
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
        val currentTourStatus = uiStateSnapshot.value.tourStatus

        Timber.d("🎯 Demo: Entered geofence for stop: ${stop?.name} (ID: $stopId)")
        Timber.d("🎯 Demo: Stop has content: ${stop?.content != null}")
        Timber.d("🎯 Demo: Content text: ${stop?.content?.text?.take(100)}")

        if (stop != null) {
            // Auto-play audio if available
            stop.content?.audioUrl?.let { audioUrl ->
                Timber.d("🎵 Demo: Playing audio for stop: ${stop.name}")
                handleAudioPlayback(audioUrl)
            }
        } else {
            Timber.w("⚠️ Demo: Stop not found for ID: $stopId")
        }

        return LiveTourPartialState.GeofenceEntered(stopId)
    }

    private fun handleTourExit() {
        cleanupResources()
    }

    private fun cleanupResources() {
        viewModelScope.launch {
            cancelContentDismissTimer()
            locationManager.stopLocationUpdates()
            geofencingManager.removeAllGeofences()
            audioPlayerManager.release()
            demoTourCoordinator.stopTourSimulation()
        }
    }

    private fun handleAudioPlayback(audioUrl: String) {
        try {
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
        Timber.d("🎯 Demo: handleGeofenceEnteredState called for stopId: $stopId")

        val updatedStops = previousState.tourStops.map { stop ->
            if (stop.id == stopId) {
                Timber.d("🎯 Demo: Activating stop: ${stop.name}")
                stop.copy(isActive = true, isVisited = true)
            } else {
                stop.copy(isActive = false)
            }
        }

        val stopIndex = updatedStops.indexOfFirst { it.id == stopId }
        val visitedCount = updatedStops.count { it.isVisited }
        val currentStop = updatedStops.find { it.id == stopId }

        Timber.d("📱 Demo: Updated state - Stop: ${currentStop?.name}, Index: $stopIndex, Visited: $visitedCount/${updatedStops.size}")
        Timber.d("📱 Demo: Current stop isActive: ${currentStop?.isActive}, hasContent: ${currentStop?.content != null}")

        val newState = previousState.copy(
            tourStops = updatedStops,
            currentStopIndex = if (stopIndex >= 0) stopIndex else previousState.currentStopIndex,
            currentStop = currentStop,
            progress = visitedCount.toFloat() / updatedStops.size,
            visitedStopsCount = visitedCount
        )

        Timber.d("📱 Demo: New state progress: ${newState.progress}, visitedCount: ${newState.visitedStopsCount}")
        return newState
    }

    private fun loadTourData(): Flow<LiveTourPartialState> = flow {
        emit(LiveTourPartialState.Loading)

        getTourByIdUseCase(tourId)
            .onSuccess { liveTour ->
                Timber.d("🎯 Loaded tour: ${liveTour.title} with ${liveTour.stops.size} stops")
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
                                geofenceRadius = 30f, // Larger radius for demo
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
                    "📍 Location update: ${location.latitude.format(6)}, ${
                        location.longitude.format(
                            6
                        )
                    }"
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
        Timber.d("🔍 Demo: Starting to observe geofence events")
        geofencingManager.geofenceEvents.collect { event ->
            Timber.d("🚪 Demo: Received geofence event: $event")
            when (event) {
                is GeofenceEvent.Enter -> {
                    Timber.d("🚪 Geofence ENTER event: ${event.geofenceId}")
                    emit(handleGeofenceEntered(event.geofenceId))
                }

                is GeofenceEvent.Exit -> {
                    Timber.d("🚪 Geofence EXIT event: ${event.geofenceId}")
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
        viewModelScope.launch {
            val stops = uiStateSnapshot.value.tourStops
            if (stops.isNotEmpty()) {
                Timber.d("🔍 Setting up geofences for ${stops.size} stops")
                val geofences = stops.map { stop ->
                    Timber.d("🔍 Geofence: ${stop.name} at ${stop.latitude}, ${stop.longitude} (radius: ${stop.geofenceRadius})")
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
                } else {
                    Timber.d("✅ Geofences setup successfully")
                }
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

    private fun startContentDismissTimer() {
        cancelContentDismissTimer()
        contentDismissJob = viewModelScope.launch {
            delay(CONTENT_DISPLAY_DURATION_MS)
            acceptIntent(LiveTourIntent.AutoDismissContent)
        }
    }

    private fun cancelContentDismissTimer() {
        contentDismissJob?.cancel()
        contentDismissJob = null
    }

    private fun checkTourCompletion(state: LiveTourUiState): LiveTourUiState {
        val allStopsVisited = state.tourStops.isNotEmpty() &&
                state.tourStops.all { it.isVisited }

        if (allStopsVisited && state.tourStatus == TourStatus.ACTIVE) {
            Timber.d("🏁 Demo: All stops visited, completing tour")
            viewModelScope.launch {
                delay(2000)
                acceptIntent(LiveTourIntent.CompleteTour)
            }
        }

        return state
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    override fun onCleared() {
        super.onCleared()
        cleanupResources()
    }

    companion object {
        private const val MAX_DEVIATION_DISTANCE_METERS = 200.0
        private const val CONTENT_DISPLAY_DURATION_MS = 5000L
    }
}

// UI State
data class LiveTourUiState(
    val tourTitle: String = "",
    val tourStops: List<LiveTourStop> = emptyList(),
    val userLocation: UserLocation? = null,
    val currentStop: LiveTourStop? = null,
    val currentStopIndex: Int = 0,
    val visitedStopsCount: Int = 0,
    val progress: Float = 0f,
    val tourStatus: TourStatus = TourStatus.UPCOMING,
    val isLocationEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val routeDeviation: RouteDeviation? = null,
    val showRouteDeviationWarning: Boolean = false,
    val audioPlayerState: AudioPlayerState? = null,
    val currentlyPlayingAudio: String? = null
) {
    // Computed property to determine if tour can be completed
    val canComplete: Boolean
        get() = tourStops.isNotEmpty() &&
                tourStops.all { it.isVisited } &&
                tourStatus == TourStatus.ACTIVE

    // Computed property to get next unvisited stop
    val nextStop: LiveTourStop?
        get() = tourStops.firstOrNull { !it.isVisited }

    // Computed property to check if all stops are visited
    val allStopsVisited: Boolean
        get() = tourStops.isNotEmpty() && tourStops.all { it.isVisited }
}

sealed class LiveTourPartialState {
    object Loading : LiveTourPartialState()

    data class TourDataLoaded(
        val title: String,
        val stops: List<LiveTourStop>
    ) : LiveTourPartialState()

    // Updated tour data (with demo content added)
    data class TourDataUpdated(
        val title: String,
        val stops: List<LiveTourStop>
    ) : LiveTourPartialState()

    // Auto-trigger location tracking
    object AutoStartLocationTracking : LiveTourPartialState()

    object LocationTrackingStarted : LiveTourPartialState()

    data class LocationUpdated(val location: UserLocation) : LiveTourPartialState()

    object TourResumed : LiveTourPartialState()

    object TourCompleted : LiveTourPartialState()

    data class GeofenceEntered(val stopId: String) : LiveTourPartialState()

    data class GeofenceExited(val stopId: String) : LiveTourPartialState()

    object ContentDismissed : LiveTourPartialState()

    data class RouteDeviationDetected(val deviation: RouteDeviation) : LiveTourPartialState()

    object RouteDeviationDismissed : LiveTourPartialState()

    data class AudioPlayerStateChanged(val state: AudioPlayerState) : LiveTourPartialState()

    data class AudioStarted(val audioUrl: String) : LiveTourPartialState()

    object LocationPermissionDenied : LiveTourPartialState()

    data class LocationServiceError(val message: String) : LiveTourPartialState()

    data class Error(val message: String) : LiveTourPartialState()

    data class TourStatusChanged(val newStatus: TourStatus) : LiveTourPartialState()
}

// Add this to your LiveTourIntent sealed class
sealed class LiveTourIntent {
    object Start : LiveTourIntent()
    object StartLocationTracking : LiveTourIntent()
    object ResumeTour : LiveTourIntent()
    object CompleteTour : LiveTourIntent()
    object ExitTour : LiveTourIntent()
    data class OnGeofenceEntered(val stopId: String) : LiveTourIntent()
    data class OnGeofenceExited(val stopId: String) : LiveTourIntent()
    object DismissContent : LiveTourIntent()
    object DismissRouteDeviation : LiveTourIntent()
    data class PlayAudio(val audioUrl: String) : LiveTourIntent()
    object PauseAudio : LiveTourIntent()
    object ResumeAudio : LiveTourIntent()
    object StopAudio : LiveTourIntent()
    data class SeekAudio(val position: Int) : LiveTourIntent()

    // New intent for auto-dismissing content
    object AutoDismissContent : LiveTourIntent()
}

// Events
sealed interface LiveTourEvent {
    data object TourCompleted : LiveTourEvent
    data object NavigateBack : LiveTourEvent
}


