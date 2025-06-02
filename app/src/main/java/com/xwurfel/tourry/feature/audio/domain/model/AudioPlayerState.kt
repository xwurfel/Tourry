package com.xwurfel.tourry.feature.audio.domain.model

import com.xwurfel.tourry.feature.audio.PlaybackState

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