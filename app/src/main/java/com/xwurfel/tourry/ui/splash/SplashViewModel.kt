package com.xwurfel.tourry.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveAuthenticationStateUseCase
import com.xwurfel.tourry.ui.main.authRoute
import com.xwurfel.tourry.ui.main.exploreRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val observeAuthenticationStateUseCase: ObserveAuthenticationStateUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    val shouldFinishSplash: StateFlow<Boolean> = combine(
        isLoading,
        destination
    ) { loading, dest ->
        !loading && dest != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    init {
        initializeSplash()
    }

    private fun initializeSplash() {
        viewModelScope.launch {
            try {
                Timber.d("Splash screen: Starting initialization")

                val minSplashTime = 750L
                val startTime = System.currentTimeMillis()

                val isAuthenticated = observeAuthenticationStateUseCase()
                    .first()

                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = maxOf(0, minSplashTime - elapsedTime)

                if (remainingTime > 0) {
                    Timber.d("Splash screen: Waiting additional ${remainingTime}ms for minimum duration")
                    delay(remainingTime)
                }

                val targetDestination = if (isAuthenticated) {
                    Timber.d("Splash screen: User authenticated, navigating to explore")
                    exploreRoute
                } else {
                    Timber.d("Splash screen: User not authenticated, navigating to auth")
                    authRoute
                }

                _destination.value = targetDestination
                _isLoading.value = false

                Timber.d("Splash screen: Initialization complete, destination: $targetDestination")

            } catch (e: Exception) {
                Timber.e(e, "Splash screen: Error during initialization")
                _destination.value = authRoute
                _isLoading.value = false
            }
        }
    }

    /**
     * Manually trigger splash completion (e.g., for testing or edge cases)
     */
    fun completeSplash() {
        viewModelScope.launch {
            if (_destination.value == null) {
                // If destination not set yet, default to auth for security
                _destination.value = authRoute
            }
            _isLoading.value = false
        }
    }

    /**
     * Reset splash state (useful for testing or if splash needs to be restarted)
     */
    fun resetSplash() {
        _isLoading.value = true
        _destination.value = null
        initializeSplash()
    }
}

/**
 * Sealed class representing splash screen states
 */
sealed class SplashState {
    object Loading : SplashState()
    data class Ready(val destination: String) : SplashState()
    data class Error(val message: String) : SplashState()
}

/**
 * Extension function to get user-friendly splash state
 */
fun SplashViewModel.getSplashState(): StateFlow<SplashState> {
    return combine(
        isLoading,
        destination
    ) { loading, dest ->
        when {
            loading -> SplashState.Loading
            dest != null -> SplashState.Ready(dest)
            else -> SplashState.Error("Failed to determine destination")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplashState.Loading
    )
}