package com.xwurfel.tourry.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveAuthenticationStateUseCase

// Values taken from the source code of corresponding Composables.
// Animating appearance / disappearance of these bars is difficult, as AnimatedVisibility
// and other similar approaches introduce weird jumping of the screen's contents.
// So a hacky and performance-heavy approach of animating hard-coded size values is used instead.
private const val BOTTOM_BAR_HEIGHT = 80
private const val NAV_RAIL_WIDTH = 80

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TourryApp(
    windowSizeClass: WindowSizeClass,
    observeAuthenticationStateUseCase: ObserveAuthenticationStateUseCase,
    modifier: Modifier = Modifier,
    appState: TourryAppState = rememberTourryAppState(
        windowSizeClass,
        observeAuthenticationStateUseCase
    ),
) {
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        // This line is present in samples, but here it causes weird bottom bar movement
        // on startup. Uncomment if its removal breaks something else.

        // contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        bottomBar = {
            val bottomBarInsets = NavigationBarDefaults.windowInsets.asPaddingValues()
            val bottomBarHeight by animateDpAsState(
                if (appState.shouldShowBottomBar) BOTTOM_BAR_HEIGHT.dp else 0.dp,
                label = "bottomBarHeight"
            )

            TemplateBottomBar(
                destinations = appState.topLevelDestinations,
                onNavigateToDestination = appState::navigateToTopLevelDestination,
                currentDestination = appState.currentDestination,
                modifier = Modifier
                    .requiredHeight(
                        bottomBarHeight +
                                bottomBarInsets.calculateBottomPadding() +
                                bottomBarInsets.calculateTopPadding()
                    )
            )
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
        ) {
            val navRailInsets = NavigationRailDefaults.windowInsets.asPaddingValues()
            val navRailWidth by animateDpAsState(
                if (appState.shouldShowNavRail) NAV_RAIL_WIDTH.dp else 0.dp,
                label = "NavRailWidth"
            )
            TemplateNavRail(
                destinations = appState.topLevelDestinations,
                onNavigateToDestination = appState::navigateToTopLevelDestination,
                currentDestination = appState.currentDestination,
                modifier = Modifier
                    .safeDrawingPadding()
                    .width(
                        navRailWidth +
                                navRailInsets.calculateLeftPadding(LocalLayoutDirection.current) +
                                navRailInsets.calculateRightPadding(LocalLayoutDirection.current)
                    )
            )

            Column(Modifier.fillMaxSize()) {
                TourryNavHost(appState = appState)
            }

            AuthenticationStateEffect(appState)
        }
    }
}

@Composable
private fun TemplateNavRail(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    TemplateNavigationRail(modifier = modifier) {
        destinations.forEach { destination ->
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            TemplateNavigationRailItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.iconTextId)) }
            )
        }
    }
}

@Composable
private fun TemplateBottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    TemplateNavigationBar(
        modifier = modifier,
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            TemplateNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.iconTextId)) }
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination) =
    this?.hierarchy?.any {
        it.route?.contains(destination.name, true) ?: false
    } ?: false
