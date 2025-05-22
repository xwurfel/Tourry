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

    val shouldShowNavigation
        @Composable get() = when (currentDestination?.route) {
            else -> true
        }

    val shouldShowBottomBar: Boolean
        @Composable get() {
            return windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && shouldShowNavigation
        }

    val shouldShowNavRail: Boolean
        @Composable get() {
            return windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && shouldShowNavigation
        }

    // TODO: replace with your own start destination
    val startDestination = flow<String> {
        // emit start destination here

    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5.seconds),
        initialValue = ""
    )

    /**
     * Map of top level destinations to be used in the BottomBar and NavRail. The key is the route.
     */
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    /**
     * UI logic for navigating to a top level destination in the app. Top level destinations have
     * only one copy of the destination of the back stack, and save and restore state whenever you
     * navigate to and from it.
     *
     * @param topLevelDestination: The destination the app needs to navigate to.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
//            popUpTo(firstRoute) {
//                saveState = true
//            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }

//        when (topLevelDestination) {
//
//        }
    }
}
