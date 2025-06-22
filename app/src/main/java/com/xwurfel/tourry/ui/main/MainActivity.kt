package com.xwurfel.tourry.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.MapsInitializer
import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveAuthenticationStateUseCase
import com.xwurfel.tourry.ui.splash.SplashViewModel
import com.xwurfel.tourry.ui.theme.TourryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var observeAuthenticationStateUseCase: ObserveAuthenticationStateUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val splashViewModel: SplashViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapsInitializer.initialize(applicationContext)

        setupSplashScreen(splashScreen)

        setContent {
            TourryTheme {
                TourryApp(
                    windowSizeClass = calculateWindowSizeClass(this),
                    observeAuthenticationStateUseCase = observeAuthenticationStateUseCase
                )
            }
        }
    }

    private fun setupSplashScreen(splashScreen: androidx.core.splashscreen.SplashScreen) {
        splashScreen.setKeepOnScreenCondition {
            val shouldKeep = !splashViewModel.shouldFinishSplash.value
            if (!shouldKeep) {
                Timber.d("Splash screen: Ready to dismiss, auth check complete")
            }
            shouldKeep
        }

        // Optional: Listen to splash screen removal
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            Timber.d("Splash screen: Exit animation started")

            // You can customize the exit animation here
            // For now, just remove it immediately
            splashScreenView.remove()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                splashViewModel.shouldFinishSplash
                    .onEach { shouldFinish ->
                        if (shouldFinish) {
                            val destination = splashViewModel.destination.value
                            Timber.d("Splash screen: Ready to navigate to $destination")
                        }
                    }
                    .collect()
            }
        }

        lifecycleScope.launch {
            kotlinx.coroutines.delay(5000) // 5 second timeout
            if (splashViewModel.isLoading.value) {
                Timber.w("Splash screen: Timeout reached, forcing completion")
                splashViewModel.completeSplash()
            }
        }
    }
}