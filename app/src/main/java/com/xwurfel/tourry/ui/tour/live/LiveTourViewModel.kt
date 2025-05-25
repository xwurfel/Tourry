package com.xwurfel.tourry.ui.tour.live

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.geofencing.GeofenceEvent
import com.xwurfel.tourry.feature.geofencing.GeofencingManager
import com.xwurfel.tourry.feature.geofencing.TourStopGeofence
import com.xwurfel.tourry.feature.location.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val geofencingManager: GeofencingManager
    // TODO: Inject tour use cases
) : MviViewModel<LiveTourUiState, LiveTourPartialState, LiveTourEvent, LiveTourIntent>(
    initialState = LiveTourUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        observeContinuousChanges(
            loadTourData(),
            observeGeofenceEvents()
        )
    }

    override fun mapIntents(intent: LiveTourIntent): Flow<LiveTourPartialState> = flow {
        when (intent) {
            LiveTourIntent.StartLocationTracking -> {
                emit(LiveTourPartialState.LocationTrackingStarted)
                startLocationService()
                setupGeofencing()
            }

            LiveTourIntent.PauseTour -> {
                emit(LiveTourPartialState.TourPaused)
                // TODO: Pause location tracking
            }

            LiveTourIntent.ResumeTour -> {
                emit(LiveTourPartialState.TourResumed)
                // TODO: Resume location tracking
            }

            LiveTourIntent.CompleteTour -> {
                emit(LiveTourPartialState.TourCompleted)
                publishEvent(LiveTourEvent.TourCompleted)
            }

            LiveTourIntent.ExitTour -> {
                emit(LiveTourPartialState.TourExited)
                publishEvent(LiveTourEvent.NavigateBack)
            }

            LiveTourIntent.DismissContent -> {
                emit(LiveTourPartialState.ContentDismissed)
            }

            is LiveTourIntent.OnGeofenceEntered -> {
                emit(LiveTourPartialState.GeofenceEntered(intent.stopId))
            }

            is LiveTourIntent.OnGeofenceExited -> {
                emit(LiveTourPartialState.GeofenceExited(intent.stopId))
            }

            is LiveTourIntent.LocationUpdated -> {
                emit(LiveTourPartialState.LocationUpdated(intent.location))
            }
        }
    }

    override fun reduceUiState(
        previousState: LiveTourUiState,
        partialState: LiveTourPartialState
    ): LiveTourUiState {
        return when (partialState) {
            LiveTourPartialState.Loading -> previousState.copy(isLoading = true)

            is LiveTourPartialState.TourDataLoaded -> previousState.copy(
                tourTitle = partialState.title,
                tourStops = partialState.stops,
                isLoading = false
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
                tourStatus = TourStatus.COMPLETED,
                progress = 1.0f
            )

            LiveTourPartialState.TourExited -> previousState

            is LiveTourPartialState.GeofenceEntered -> {
                val updatedStops = previousState.tourStops.map { stop ->
                    if (stop.id == partialState.stopId) {
                        stop.copy(isActive = true)
                    } else {
                        stop.copy(isActive = false)
                    }
                }
                val stopIndex = updatedStops.indexOfFirst { it.id == partialState.stopId }
                previousState.copy(
                    tourStops = updatedStops,
                    currentStopIndex = if (stopIndex >= 0) stopIndex else previousState.currentStopIndex,
                    currentStop = updatedStops.find { it.id == partialState.stopId },
                    progress = if (stopIndex >= 0) (stopIndex + 1).toFloat() / updatedStops.size else previousState.progress
                )
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

            is LiveTourPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private fun loadTourData(): Flow<LiveTourPartialState> = flow {
        emit(LiveTourPartialState.Loading)
        try {
            // TODO: Load from repository
            kotlinx.coroutines.delay(1000)

            // Mock data
            val mockStops = listOf(
                LiveTourStop(
                    id = "1",
                    name = "Starting Point",
                    latitude = 48.8566,
                    longitude = 2.3522,
                    geofenceRadius = 50f,
                    content = StopContent(
                        text = "Welcome to our amazing tour! This is where we begin our journey through the historic heart of the city.",
                        imageUrl = null,
                        audioUrl = null
                    )
                ),
                LiveTourStop(
                    id = "2",
                    name = "Historic Square",
                    latitude = 48.8576,
                    longitude = 2.3532,
                    geofenceRadius = 50f,
                    content = StopContent(
                        text = "This beautiful square has been the center of city life for over 300 years. Notice the architectural details on the surrounding buildings.",
                        imageUrl = null,
                        audioUrl = null
                    )
                ),
                LiveTourStop(
                    id = "3",
                    name = "Local Market",
                    latitude = 48.8586,
                    longitude = 2.3542,
                    geofenceRadius = 50f,
                    content = StopContent(
                        text = "This vibrant market showcases the best of local produce and crafts. Feel free to explore and interact with the vendors.",
                        imageUrl = null,
                        audioUrl = null
                    )
                )
            )

            emit(LiveTourPartialState.TourDataLoaded("Amazing City Tour", mockStops))
        } catch (e: Exception) {
            emit(LiveTourPartialState.Error("Failed to load tour data"))
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

    private fun startLocationService() {
        val intent = LocationService.getStartIntent(
            context,
            tourId,
            uiStateSnapshot.value.tourTitle
        )
        context.startForegroundService(intent)
    }

    private fun stopLocationService() {
        val intent = LocationService.getStopIntent(context)
        context.stopService(intent)
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

    override fun onCleared() {
        super.onCleared()
        stopLocationService()
        geofencingManager.removeAllGeofences()
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
    val isLocationEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface LiveTourPartialState {
    object Loading : LiveTourPartialState
    data class TourDataLoaded(
        val title: String,
        val stops: List<LiveTourStop>
    ) : LiveTourPartialState

    object LocationTrackingStarted : LiveTourPartialState
    data class LocationUpdated(val location: UserLocation) : LiveTourPartialState
    object TourPaused : LiveTourPartialState
    object TourResumed : LiveTourPartialState
    object TourCompleted : LiveTourPartialState
    object TourExited : LiveTourPartialState
    data class GeofenceEntered(val stopId: String) : LiveTourPartialState
    data class GeofenceExited(val stopId: String) : LiveTourPartialState
    object ContentDismissed : LiveTourPartialState
    data class Error(val message: String) : LiveTourPartialState
}

sealed interface LiveTourIntent {
    object StartLocationTracking : LiveTourIntent
    object PauseTour : LiveTourIntent
    object ResumeTour : LiveTourIntent
    object CompleteTour : LiveTourIntent
    object ExitTour : LiveTourIntent
    object DismissContent : LiveTourIntent
    data class OnGeofenceEntered(val stopId: String) : LiveTourIntent
    data class OnGeofenceExited(val stopId: String) : LiveTourIntent
    data class LocationUpdated(val location: UserLocation) : LiveTourIntent
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
    val content: StopContent? = null
)

data class StopContent(
    val text: String,
    val imageUrl: String? = null,
    val audioUrl: String? = null
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TourStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}