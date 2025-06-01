package com.xwurfel.tourry.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber

/**
 * Effect that observes authentication state changes and handles navigation accordingly.
 * This ensures that when a user signs out from any screen, they are automatically
 * redirected to the authentication screen.
 *
 * @param appState The app state containing navigation methods
 */
@Composable
fun AuthenticationStateEffect(
    appState: TourryAppState,
) {
    val isAuthenticated = appState.observeAuthenticationStateUseCase()
        .collectAsStateWithLifecycle(initialValue = null)

    val currentRoute = appState.currentDestination?.route
    LaunchedEffect(isAuthenticated.value) {
        val authState = isAuthenticated.value
        // Only react to authentication state changes, not initial load
        if (authState != null) {
            when {
                // User signed out while in authenticated area
                !authState && currentRoute != authRoute -> {
                    Timber.d("User signed out, navigating to authentication")
                    appState.navigateToAuthentication()
                }

                // User signed in while on auth screen
                authState && currentRoute == authRoute -> {
                    Timber.d("User signed in, navigating to authenticated area")
                    appState.navigateToAuthenticatedArea()
                }
            }
        }
    }
}

/**
 * Helper function to check if the current route requires authentication
 */
fun isAuthenticatedRoute(route: String?): Boolean {
    return when (route) {
        authRoute -> false
        null -> false
        else -> true
    }
}

/**
 * Helper function to get a user-friendly description of authentication requirement
 */
fun getAuthRequirementMessage(route: String?): String? {
    return when (route) {
        myToursRoute -> "Sign in to view your tours"
        profileRoute -> "Sign in to access your profile"
        tourCreationRoute, tourCreationRouteWithArgs -> "Sign in to create tours"
        liveTourRouteWithArgs -> "Sign in to join live tours"
        else -> null
    }
}