package com.xwurfel.tourry.ui.tour.live

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.geofencing.GeofenceEvent
import com.xwurfel.tourry.feature.geofencing.GeofencingManager
import com.xwurfel.tourry.feature.geofencing.TourStopGeofence
import com.xwurfel.tourry.feature.location.service.LocationService
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class LiveTourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val geofencingManager: GeofencingManager,
    private val mockDataManager: MockDataManager
) : MviViewModel<LiveTourUiState, LiveTourPartialState, LiveTourEvent, LiveTourIntent>(
    initialState = LiveTourUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        observeContinuousChanges(
            loadTourData(),
            observeGeofenceEvents(),
            simulateLocationUpdates() // For mock purposes
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
                stopLocationService()
            }

            LiveTourIntent.ResumeTour -> {
                emit(LiveTourPartialState.TourResumed)
                startLocationService()
            }

            LiveTourIntent.CompleteTour -> {
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
        }
    }

    override fun reduceUiState(
        previousState: LiveTourUiState,
        partialState: LiveTourPartialState
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
                tourStatus = TourStatus.COMPLETED,
                progress = 1.0f
            )

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
                        longitude = firstStop.longitude + 0.0001,
                        accuracy = 10f
                    )
                )
            )

            // Simulate moving through stops every 30 seconds for demo
            kotlinx.coroutines.delay(5000)
            stops.forEach { stop ->
                emit(
                    LiveTourPartialState.LocationUpdated(
                        UserLocation(
                            latitude = stop.latitude,
                            longitude = stop.longitude,
                            accuracy = 5f
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
    val visitedStopsCount: Int = 0,
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