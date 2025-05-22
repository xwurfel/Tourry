package com.xwurfel.tourry.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * This class is a base class for ViewModels implementing the Model-View-Intent (MVI) architecture.
 *
 * The ViewModel stops collecting data from flows when [uiState] is not observed,
 * provided that [observeContinuousChanges] is used to subscribe the ViewModel
 * to flows from UseCases, Repositories and all other sources.
 *
 * [PARTIAL_UI_STATE] is used to produce the new [UI_STATE] through the [reduceUiState] function.
 * There are two sources of [PARTIAL_UI_STATE]: [userIntents] and [continuousFlows].
 *
 * The View uses the [userIntents] flow.
 * The View submits [INTENT]s via the [acceptIntent] function.
 * The [INTENT]s are then mapped into [PARTIAL_UI_STATE]s in the [mapIntents] function.
 * The resulting [PARTIAL_UI_STATE]s go into the [userIntents] flow.
 *
 * The ViewModel uses the [continuousFlows] flow.
 * The ViewModel submits flows producing [PARTIAL_UI_STATE]s
 * through the [observeContinuousChanges] function.
 * These flows then go into the [continuousFlows] flow.
 *
 * @param UI_STATE represents the UI state of a specific screen. Usually, it's a data class
 * @param PARTIAL_UI_STATE represents all possible events that can alter the state.
 * It is the second argument in the [reduceUiState], the first being the old state.
 * Usually, it's a sealed interface.
 * @param EVENT represents one-time events transmitted
 * from the ViewModel to the View, such as navigation.
 * Basically it's our good old SingleLiveEvent.
 * Usually, it's a sealed interface.
 * Using it is optional. In screens that do not need it, use [Nothing] instead of the actual type.
 * @param INTENT represents events transmitted from the View to the ViewModel, such as button clicks.
 * These are mapped into [PARTIAL_UI_STATE] in the [mapIntents] function.
 * Usually, it's a sealed interface.
 * @param initialState the initial UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MviViewModel<UI_STATE, PARTIAL_UI_STATE, EVENT, INTENT>(
    initialState: UI_STATE,
) : ViewModel() {
    private val intentFlow = MutableSharedFlow<INTENT>()
    private val continuousPartialStateFlow =
        MutableStateFlow<List<Flow<PARTIAL_UI_STATE>>>(emptyList())

    private val intentFlowListenerStarted = CompletableDeferred<Unit>()
    private val continuousPartialStateFlowListenerStarted = CompletableDeferred<Unit>()

    private val _uiStateSnapshot = MutableStateFlow(initialState)

    /**
     * The flow of UI state which can safely be observed forever.
     * It only contains a copy of the latest UI state,
     * so observing this flow will still allow [uiState] and [continuousPartialStateFlow] to stop.
     */
    protected val uiStateSnapshot = _uiStateSnapshot.asStateFlow()

    /**
     * The flow of UI state which should be accessed only from the UI.
     * Will stop without subscribers, stopping every flow of [continuousPartialStateFlow] as well.
     */
    val uiState = merge(
        userIntents(),
        continuousFlows().flatMapConcat { it.merge() },
    )
        .onEach { Timber.d("New partial state:\n$it") }
        .scan(initialState, ::reduceUiState)
        .onEach { _uiStateSnapshot.value = it }
        .onEach { Timber.d("New state:\n$it") }
        .catch { Timber.e(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), initialState)

    private val eventChannel = Channel<EVENT>(Channel.BUFFERED)
    val event = eventChannel.receiveAsFlow()

    private fun userIntents(): Flow<PARTIAL_UI_STATE> =
        intentFlow
            .onStart { intentFlowListenerStarted.complete(Unit) }
            .flatMapConcat(::mapIntents)

    private fun continuousFlows(): Flow<List<Flow<PARTIAL_UI_STATE>>> =
        continuousPartialStateFlow
            .onStart { continuousPartialStateFlowListenerStarted.complete(Unit) }

    fun acceptIntent(intent: INTENT) {
        viewModelScope.launch {
            intentFlowListenerStarted.await()
            intentFlow.emit(intent)
        }
    }

    protected fun observeContinuousChanges(vararg changesFlow: Flow<PARTIAL_UI_STATE>) {
        viewModelScope.launch {
            continuousPartialStateFlowListenerStarted.await()
            continuousPartialStateFlow.update { it + changesFlow }
        }
    }

    protected fun publishEvent(event: EVENT) {
        viewModelScope.launch {
            eventChannel.send(event)
        }
    }

    protected abstract fun mapIntents(intent: INTENT): Flow<PARTIAL_UI_STATE>

    protected abstract fun reduceUiState(
        previousState: UI_STATE,
        partialState: PARTIAL_UI_STATE,
    ): UI_STATE
}
