package com.xwurfel.tourry.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveAuthenticationStateUseCase

/**
 * Composable that provides the current authentication state
 *
 * @param ObserveAuthenticationStateUseCase Use case to observe authentication
 * @param initialValue Initial value while loading (defaults to false for security)
 * @return Current authentication state
 */
@Composable
fun ObserveAuthenticationStateUseCase.collectAsState(
    initialValue: Boolean = false
): Boolean {
    val authState by this().collectAsStateWithLifecycle(initialValue = initialValue)
    return authState
}

/**
 * Extension to check if user can access a specific route
 */
fun TourryAppState.canAccessRoute(route: String, isAuthenticated: Boolean): Boolean {
    return when (route) {
        authRoute -> true // Auth route is always accessible
        exploreRoute -> true // Explore is public (though some features require auth)
        myToursRoute, profileRoute -> isAuthenticated // These require authentication
        tourCreationRoute, tourCreationRouteWithArgs -> isAuthenticated // Creating tours requires auth
        liveTourRouteWithArgs -> isAuthenticated // Live tours require auth for tracking
        tourSummaryRouteWithArgs -> isAuthenticated // Summary requires auth for feedback
        else -> isAuthenticated // Default to requiring auth for new routes
    }
}

/**
 * Extension to get redirect route when authentication is required
 */
fun TourryAppState.getRedirectRoute(
    targetRoute: String,
    isAuthenticated: Boolean
): String {
    return if (canAccessRoute(targetRoute, isAuthenticated)) {
        targetRoute
    } else {
        authRoute
    }
}

/**
 * Safe navigation that checks authentication before navigating
 */
fun TourryAppState.navigateWithAuthCheck(
    route: String,
    isAuthenticated: Boolean,
    onAuthRequired: (() -> Unit)? = null
) {
    if (canAccessRoute(route, isAuthenticated)) {
        navController.navigate(route)
    } else {
        onAuthRequired?.invoke() ?: navigateToAuthentication()
    }
}

/**
 * Authentication requirement levels for different features
 */
enum class AuthRequirement {
    NONE,           // Feature accessible to everyone
    OPTIONAL,       // Feature better with auth but works without
    REQUIRED,       // Feature requires authentication
    REQUIRED_VERIFIED // Feature requires verified authentication
}

/**
 * Get authentication requirement for a route
 */
fun getAuthRequirement(route: String?): AuthRequirement {
    return when (route) {
        authRoute -> AuthRequirement.NONE
        exploreRoute -> AuthRequirement.OPTIONAL // Can browse but need auth to join
        tourDetailRouteWithArgs -> AuthRequirement.OPTIONAL // Can view but need auth to join
        myToursRoute, profileRoute -> AuthRequirement.REQUIRED
        tourCreationRoute, tourCreationRouteWithArgs -> AuthRequirement.REQUIRED
        liveTourRouteWithArgs -> AuthRequirement.REQUIRED
        tourSummaryRouteWithArgs -> AuthRequirement.REQUIRED
        else -> AuthRequirement.REQUIRED // Safe default
    }
}

/**
 * Helper to determine if current navigation should be restricted
 */
@Composable
fun shouldRestrictNavigation(
    currentRoute: String?,
    isAuthenticated: Boolean
): Boolean {
    val requirement = getAuthRequirement(currentRoute)
    return requirement == AuthRequirement.REQUIRED && !isAuthenticated
}