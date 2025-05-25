package com.xwurfel.tourry.feature.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentAudioUrl: String? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume but continue playing
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume normal volume
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }

    fun loadAudio(audioUrl: String) {
        if (currentAudioUrl == audioUrl && mediaPlayer != null) {
            // Audio already loaded
            return
        }

        release()
        currentAudioUrl = audioUrl

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(context, audioUrl.toUri())

                setOnPreparedListener { player ->
                    _duration.value = player.duration
                    _playbackState.value = PlaybackState.PREPARED
                }

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.COMPLETED
                    releaseAudioFocus()
                }

                setOnErrorListener { _, what, extra ->
                    _playbackState.value = PlaybackState.ERROR
                    true
                }

                prepareAsync()
                _playbackState.value = PlaybackState.LOADING
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
        }
    }

    fun play() {
        val player = mediaPlayer ?: return

        if (requestAudioFocus()) {
            try {
                player.start()
                _playbackState.value = PlaybackState.PLAYING
                startPositionUpdates()
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.ERROR
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            _playbackState.value = PlaybackState.STOPPED
            _currentPosition.value = 0
            releaseAudioFocus()
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(position)
            _currentPosition.value = position
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentAudioUrl = null
        _playbackState.value = PlaybackState.IDLE
        _currentPosition.value = 0
        _duration.value = 0
        releaseAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.abandonAudioFocusRequest(request)
        }
    }

    private fun startPositionUpdates() {
        // TODO: Implement position updates using coroutines
        // This could be done with a coroutine that updates position every second
    }
}

enum class PlaybackState {
    IDLE,
    LOADING,
    PREPARED,
    PLAYING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}