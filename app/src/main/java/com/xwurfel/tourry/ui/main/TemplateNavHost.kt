package com.xwurfel.tourry.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xwurfel.tourry.ui.explore.ExploreRoute
import com.xwurfel.tourry.ui.navigation.*

@Composable
fun TemplateNavHost(
    appState: TourryAppState,
    modifier: Modifier = Modifier,
    startDestination: String = appState.startDestination.collectAsStateWithLifecycle().value,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // Bottom Navigation Destinations
        composable(exploreRoute) {
            ExploreRoute(
                onNavigateToTourDetail = { tourId ->
                    navController.navigate("$tourDetailRoute/$tourId")
                },
                onNavigateToTourCreation = {
                    navController.navigate(tourCreationRoute)
                }
            )
        }

        composable(myToursRoute) {
            MyToursRoute(
                onNavigateToTourDetail = { tourId ->
                    navController.navigate("$tourDetailRoute/$tourId")
                },
                onNavigateToLiveTour = { tourId ->
                    navController.navigate("$liveTourRoute/$tourId")
                },
                onNavigateToTourSummary = { tourId ->
                    navController.navigate("$tourSummaryRoute/$tourId")
                },
                onNavigateToTourEdit = { tourId ->
                    navController.navigate("$tourCreationRoute?tourId=$tourId")
                }
            )
        }

        composable(profileRoute) {
            ProfileRoute(
                onNavigateToAuth = {
                    navController.navigate(authRoute)
                }
            )
        }

        // Other Destinations
        composable(authRoute) {
            AuthRoute(
                onAuthSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = tourDetailRouteWithArgs,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            TourDetailRoute(
                tourId = tourId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLiveTour = {
                    navController.navigate("$liveTourRoute/$tourId")
                },
                onNavigateToBooking = {
                    // TODO: Implement booking flow
                }
            )
        }

        composable(
            route = tourCreationRouteWithArgs,
            arguments = listOf(
                navArgument("tourId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TourCreationRoute(
                onNavigateBack = { navController.popBackStack() },
                onTourCreated = { tourId ->
                    navController.navigate("$tourDetailRoute/$tourId") {
                        popUpTo(myToursRoute)
                    }
                }
            )
        }

        composable(
            route = liveTourRouteWithArgs,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            LiveTourRoute(
                tourId = tourId,
                onTourCompleted = {
                    navController.navigate("$tourSummaryRoute/$tourId") {
                        popUpTo(liveTourRoute) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = tourSummaryRouteWithArgs,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            TourSummaryRoute(
                tourId = tourId,
                onNavigateHome = {
                    navController.navigate(exploreRoute) {
                        popUpTo(exploreRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}