package com.xwurfel.tourry.ui.tour.mine

import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.core.domain.util.getOrNull
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.profile.domain.usecase.GetCurrentUserIdUseCase
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourByIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveToursByAuthorUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveUserParticipationsUseCase
import com.xwurfel.tourry.ui.tour.mine.mapper.MyTourMapper.toMyTour
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MyToursViewModel @Inject constructor(
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val observeUserParticipationsUseCase: ObserveUserParticipationsUseCase,
    private val observeToursByAuthorUseCase: ObserveToursByAuthorUseCase,
    private val getTourByIdUseCase: GetTourByIdUseCase,
    private val tourRepository: TourRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MviViewModel<MyToursUiState, MyToursPartialState, MyToursEvent, MyToursIntent>(
    initialState = MyToursUiState()
) {

    init {
        observeContinuousChanges(
            loadMyTours()
        )
    }

    override fun mapIntents(intent: MyToursIntent): Flow<MyToursPartialState> = flow {
        when (intent) {
            is MyToursIntent.TabChanged -> {
                emit(MyToursPartialState.TabChanged(intent.tab))
            }

            is MyToursIntent.TourClicked -> {
                when (intent.tourStatus) {
                    TourStatus.LIVE -> publishEvent(MyToursEvent.NavigateToLiveTour(intent.tourId))
                    TourStatus.UPCOMING -> publishEvent(MyToursEvent.NavigateToTourDetail(intent.tourId))
                    TourStatus.COMPLETED -> publishEvent(MyToursEvent.NavigateToTourSummary(intent.tourId))
                }
            }

            is MyToursIntent.EditTour -> {
                publishEvent(MyToursEvent.NavigateToTourEdit(intent.tourId))
            }

            is MyToursIntent.CancelTour -> {
                emit(MyToursPartialState.Loading)

                tourRepository.deleteTour(intent.tourId)
                    .onSuccess {
                        emit(MyToursPartialState.TourCancelled(intent.tourId))
                    }
                    .onFailure { error ->
                        emit(MyToursPartialState.Error("Failed to cancel tour: ${error.msg}"))
                    }
            }

            MyToursIntent.RefreshTours -> {
                emit(MyToursPartialState.Loading)
                // Refresh will be handled by the continuous flow
            }
        }
    }

    override fun reduceUiState(
        previousState: MyToursUiState,
        partialState: MyToursPartialState
    ): MyToursUiState {
        return when (partialState) {
            is MyToursPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is MyToursPartialState.ToursLoaded -> previousState.copy(
                joinedTours = partialState.joinedTours,
                createdTours = partialState.createdTours,
                isLoading = false,
                error = null
            )

            is MyToursPartialState.TabChanged -> previousState.copy(
                selectedTab = partialState.tab
            )

            is MyToursPartialState.TourCancelled -> {
                val updatedCreatedTours =
                    previousState.createdTours.filterNot { it.id == partialState.tourId }
                previousState.copy(
                    createdTours = updatedCreatedTours,
                    isLoading = false
                )
            }

            is MyToursPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadMyTours(): Flow<MyToursPartialState> =
        getCurrentUserIdUseCase()
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                Timber.d("MyTours: Loading tours for user: $userId")

                combine(
                    loadJoinedTours(userId),
                    loadCreatedTours(userId)
                ) { joinedTours, createdTours ->
                    Timber.d("MyTours: Combined - Joined: ${joinedTours.size}, Created: ${createdTours.size}")
                    MyToursPartialState.ToursLoaded(joinedTours, createdTours)
                }
            }
            .catch { error ->
                Timber.e(error, "MyTours: Error in loadMyTours flow")
                MyToursPartialState.Error("Failed to load tours: ${error.message}")
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadJoinedTours(userId: String): Flow<List<MyTour>> {
        return observeUserParticipationsUseCase(userId)
            .map { participations ->
                Timber.d("MyTours: Got ${participations.size} participations")
                participations
            }
            .flatMapLatest { participations ->
                flow {
                    val joinedTours = mutableListOf<MyTour>()

                    for (participation in participations) {
                        try {
                            val tour = withContext(ioDispatcher) {
                                getTourByIdUseCase(participation.tourId).getOrNull()
                            }

                            tour?.let {
                                val myTour = participation.toMyTour(it)
                                myTour?.let { joinedTours.add(it) }
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "MyTours: Failed to load tour ${participation.tourId}")
                        }
                    }

                    Timber.d("MyTours: Successfully loaded ${joinedTours.size} joined tours")
                    emit(joinedTours.toList())
                }
            }
            .flowOn(ioDispatcher)
            .catch { error ->
                Timber.e(error, "MyTours: Error loading joined tours")
                emit(emptyList())
            }
    }

    private fun loadCreatedTours(userId: String): Flow<List<MyTour>> {
        return observeToursByAuthorUseCase(userId)
            .map { tours ->
                Timber.d("MyTours: Got ${tours.size} created tours")
                tours.map { tour ->
                    tour.toMyTour()
                }
            }
            .catch { error ->
                Timber.e(error, "MyTours: Error loading created tours")
                emit(emptyList())
            }
    }
}

// States
data class MyToursUiState(
    val selectedTab: MyToursTab = MyToursTab.JOINED,
    val joinedTours: List<MyTour> = emptyList(),
    val createdTours: List<MyTour> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface MyToursPartialState {
    object Loading : MyToursPartialState
    data class ToursLoaded(
        val joinedTours: List<MyTour>, val createdTours: List<MyTour>
    ) : MyToursPartialState

    data class TabChanged(val tab: MyToursTab) : MyToursPartialState
    data class TourCancelled(val tourId: String) : MyToursPartialState
    data class Error(val message: String) : MyToursPartialState
}

sealed interface MyToursIntent {
    data class TabChanged(val tab: MyToursTab) : MyToursIntent
    data class TourClicked(val tourId: String, val tourStatus: TourStatus) : MyToursIntent
    data class EditTour(val tourId: String) : MyToursIntent
    data class CancelTour(val tourId: String) : MyToursIntent
    object RefreshTours : MyToursIntent
}

sealed interface MyToursEvent {
    data class NavigateToTourDetail(val tourId: String) : MyToursEvent
    data class NavigateToLiveTour(val tourId: String) : MyToursEvent
    data class NavigateToTourSummary(val tourId: String) : MyToursEvent
    data class NavigateToTourEdit(val tourId: String) : MyToursEvent
}

// Data models
enum class MyToursTab {
    JOINED, CREATED
}

data class MyTour(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val startTime: Long,
    val status: TourStatus,
    val participantsCount: Int = 0,
    val rating: Float? = null
)

enum class TourStatus {
    UPCOMING, LIVE, COMPLETED
}