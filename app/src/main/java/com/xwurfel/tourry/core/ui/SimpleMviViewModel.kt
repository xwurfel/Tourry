package com.xwurfel.tourry.core.ui

abstract class SimpleMviViewModel<UI_STATE, EVENT, INTENT>(
    initialState: UI_STATE
) : MviViewModel<UI_STATE, (UI_STATE) -> UI_STATE, EVENT, INTENT>(initialState) {
    override fun reduceUiState(
        previousState: UI_STATE,
        partialState: (UI_STATE) -> UI_STATE,
    ): UI_STATE = partialState.invoke(previousState)
}
