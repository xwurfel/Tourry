package com.xwurfel.tourry.ui.main

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberTourryAppState(
    windowSizeClass: WindowSizeClass,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): TourryAppState {
    return remember(
        navController,
        windowSizeClass,
    ) {
        TourryAppState(
            navController,
            windowSizeClass,
            coroutineScope,
        )
    }
}

@Stable
class TourryAppState(
    val navController: NavHostController,
    val windowSizeClass: WindowSizeClass,
    coroutineScope: CoroutineScope,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val shouldShowNavigation: Boolean
        @Composable get() {
            val route = currentDestination?.route
            return when (route) {
                authRoute -> false
                liveTourRouteWithArgs -> false
                tourSummaryRouteWithArgs -> false
                // Hide navigation during tour creation for focused experience
                tourCreationRoute -> false
                tourCreationRouteWithArgs -> false
                else -> true
            }
        }

    val shouldShowBottomBar: Boolean
        @Composable get() {
            return windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && shouldShowNavigation
        }

    val shouldShowNavRail: Boolean
        @Composable get() {
            return windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && shouldShowNavigation
        }

    val startDestination = flow<String> {
        // TODO: Check if user is authenticated
        // For now, always start with explore
        emit(exploreRoute)
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5.seconds),
        initialValue = exploreRoute
    )

    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }

        when (topLevelDestination) {
            TopLevelDestination.EXPLORE -> navController.navigate(exploreRoute, topLevelNavOptions)
            TopLevelDestination.MY_TOURS -> navController.navigate(myToursRoute, topLevelNavOptions)
            TopLevelDestination.PROFILE -> navController.navigate(profileRoute, topLevelNavOptions)
        }
    }

    /**
     * Helper function to check if current destination is a top level destination
     */
    @Composable
    fun isTopLevelDestination(): Boolean {
        val route = currentDestination?.route
        return route in listOf(exploreRoute, myToursRoute, profileRoute)
    }
}