package com.xwurfel.tourry.ui.tour.detail

import android.content.Context
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.location.LocationManager
import com.xwurfel.tourry.feature.profile.domain.usecase.GetCurrentUserIdUseCase
import com.xwurfel.tourry.feature.tours.domain.model.TourDetail
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourByIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.JoinTourUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveUserParticipationsUseCase
import com.xwurfel.tourry.ui.tour.detail.mapper.TourDetailMapper.toTourDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TourDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTourByIdUseCase: GetTourByIdUseCase,
    private val joinTourUseCase: JoinTourUseCase,
    private val observeUserParticipationsUseCase: ObserveUserParticipationsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val tourAnalytics: TourAnalytics,
    @ApplicationContext private val context: Context,
    locationManager: LocationManager,
) : MviViewModel<TourDetailUiState, TourDetailPartialState, TourDetailEvent, TourDetailIntent>(
    initialState = TourDetailUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        tourAnalytics.trackEvent(
            "tour_detail_opened",
            mapOf("tour_id" to tourId)
        )

        observeContinuousChanges(
            loadTourDetail(),
            observeJoinedStatus(),
            flow {
                locationManager.getLastKnownLocation()
                    ?.let { emit(TourDetailPartialState.UserLocationRetrieved(it)) }
            }
        )
    }

    override fun mapIntents(intent: TourDetailIntent): Flow<TourDetailPartialState> = flow {
        when (intent) {
            TourDetailIntent.JoinTour -> {
                emit(TourDetailPartialState.JoiningTour)
                tourAnalytics.trackEvent(
                    "tour_join_attempted",
                    mapOf("tour_id" to tourId)
                )

                joinTourUseCase(tourId)
                    .onSuccess {
                        val tour = uiStateSnapshot.value.tour
                        tourAnalytics.trackEvent(
                            "tour_joined",
                            mapOf(
                                "tour_id" to tourId,
                                "tour_price" to (tour?.price?.toString() ?: "0"),
                                "join_method" to "detail_page"
                            )
                        )

                        emit(TourDetailPartialState.TourJoined)

                        if (tour?.isLive == true) {
                            publishEvent(TourDetailEvent.NavigateToLiveTour)
                        } else {
                            publishEvent(TourDetailEvent.NavigateToBooking)
                        }
                    }
                    .onFailure { error ->
                        emit(
                            TourDetailPartialState.Error(
                                "Failed to join tour: ${
                                    error.msg.asString(
                                        context.resources
                                    )
                                }"
                            )
                        )
                    }
            }

            TourDetailIntent.StartTour -> {
                tourAnalytics.trackEvent(
                    "tour_started_from_detail",
                    mapOf("tour_id" to tourId)
                )

                val tour = uiStateSnapshot.value.tour
                if (tour?.isJoined == true) {
                    publishEvent(TourDetailEvent.NavigateToLiveTour)
                } else {
                    emit(TourDetailPartialState.Error("You must join the tour first"))
                }
            }

            TourDetailIntent.ShareTour -> {
                tourAnalytics.trackEvent(
                    "tour_shared",
                    mapOf("tour_id" to tourId, "share_source" to "detail_page")
                )
                emit(TourDetailPartialState.TourShared)
            }

            TourDetailIntent.RefreshTour -> {
                emit(TourDetailPartialState.Loading)
                // Reload tour data
                getTourByIdUseCase(tourId)
                    .onSuccess { tour ->
                        val isJoined = uiStateSnapshot.value.tour?.isJoined ?: false
                        emit(TourDetailPartialState.TourLoaded(tour.toTourDetail(isJoined = isJoined)))
                    }
                    .onFailure { error ->
                        emit(
                            TourDetailPartialState.Error(
                                "Failed to refresh tour: ${
                                    error.msg.asString(
                                        context.resources
                                    )
                                }"
                            )
                        )
                    }
            }
        }
    }

    override fun reduceUiState(
        previousState: TourDetailUiState,
        partialState: TourDetailPartialState
    ): TourDetailUiState {
        return when (partialState) {
            is TourDetailPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is TourDetailPartialState.TourLoaded -> previousState.copy(
                tour = partialState.tour,
                isLoading = false,
                error = null
            )

            is TourDetailPartialState.JoiningTour -> previousState.copy(
                isJoining = true,
                error = null
            )

            is TourDetailPartialState.TourJoined -> {
                val updatedTour = previousState.tour?.copy(isJoined = true)
                previousState.copy(
                    tour = updatedTour,
                    isJoining = false
                )
            }

            is TourDetailPartialState.JoinedStatusUpdated -> {
                val updatedTour = previousState.tour?.copy(isJoined = partialState.isJoined)
                previousState.copy(tour = updatedTour)
            }

            is TourDetailPartialState.TourShared -> previousState.copy(
                error = null // Could show a success message here
            )

            is TourDetailPartialState.Error -> previousState.copy(
                isLoading = false,
                isJoining = false,
                error = partialState.message
            )

            is TourDetailPartialState.UserLocationRetrieved -> previousState.copy(
                userLocation = partialState.location
            )
        }
    }

    private fun loadTourDetail(): Flow<TourDetailPartialState> = flow {
        emit(TourDetailPartialState.Loading)

        getTourByIdUseCase(tourId)
            .onSuccess { tour ->
                val isJoined = checkIfUserJoinedTour()
                emit(TourDetailPartialState.TourLoaded(tour.toTourDetail(isJoined = isJoined)))
            }
            .onFailure { error ->
                emit(
                    TourDetailPartialState.Error(
                        "Failed to load tour: ${
                            error.msg.asString(
                                context.resources
                            )
                        }"
                    )
                )
            }
    }

    private fun observeJoinedStatus(): Flow<TourDetailPartialState> = flow {
        getCurrentUserIdUseCase()
            .filterNotNull()
            .collect { userId ->
                observeUserParticipationsUseCase(userId)
                    .map { participations ->
                        participations.any {
                            it.tourId == tourId &&
                                    it.status == com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus.JOINED
                        }
                    }
                    .collect { isJoined ->
                        emit(TourDetailPartialState.JoinedStatusUpdated(isJoined))
                    }
            }
    }

    private suspend fun checkIfUserJoinedTour(): Boolean {
        // This is a simple check that will be updated by the observeJoinedStatus flow
        return false
    }
}

// States
data class TourDetailUiState(
    val tour: TourDetail? = null,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null,
    val userLocation: Location? = null
)

sealed interface TourDetailPartialState {
    data object Loading : TourDetailPartialState
    data class TourLoaded(val tour: TourDetail) : TourDetailPartialState
    data object JoiningTour : TourDetailPartialState
    data object TourJoined : TourDetailPartialState
    data class JoinedStatusUpdated(val isJoined: Boolean) : TourDetailPartialState
    data object TourShared : TourDetailPartialState
    data class Error(val message: String) : TourDetailPartialState
    data class UserLocationRetrieved(val location: Location) : TourDetailPartialState
}

sealed interface TourDetailIntent {
    data object JoinTour : TourDetailIntent
    data object StartTour : TourDetailIntent
    data object RefreshTour : TourDetailIntent
    data object ShareTour : TourDetailIntent
}

sealed interface TourDetailEvent {
    data object NavigateToLiveTour : TourDetailEvent
    data object NavigateToBooking : TourDetailEvent
}

