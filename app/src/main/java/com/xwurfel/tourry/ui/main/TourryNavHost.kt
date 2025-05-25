package com.xwurfel.tourry.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xwurfel.tourry.ui.auth.AuthRoute
import com.xwurfel.tourry.ui.explore.ExploreRoute
import com.xwurfel.tourry.ui.navigation.authRoute
import com.xwurfel.tourry.ui.navigation.exploreRoute
import com.xwurfel.tourry.ui.navigation.liveTourRoute
import com.xwurfel.tourry.ui.navigation.liveTourRouteWithArgs
import com.xwurfel.tourry.ui.navigation.myToursRoute
import com.xwurfel.tourry.ui.navigation.profileRoute
import com.xwurfel.tourry.ui.navigation.tourCreationRoute
import com.xwurfel.tourry.ui.navigation.tourCreationRouteWithArgs
import com.xwurfel.tourry.ui.navigation.tourDetailRoute
import com.xwurfel.tourry.ui.navigation.tourDetailRouteWithArgs
import com.xwurfel.tourry.ui.navigation.tourSummaryRoute
import com.xwurfel.tourry.ui.navigation.tourSummaryRouteWithArgs
import com.xwurfel.tourry.ui.profile.ProfileRoute
import com.xwurfel.tourry.ui.tour.creation.TourCreationRoute
import com.xwurfel.tourry.ui.tour.detail.TourDetailRoute
import com.xwurfel.tourry.ui.tour.live.LiveTourRoute
import com.xwurfel.tourry.ui.tour.mine.MyToursRoute
import com.xwurfel.tourry.ui.tour.summary.TourSummaryRoute

@Composable
fun TourryNavHost(
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
                    navController.navigate(authRoute) {
                        popUpTo(profileRoute) { inclusive = false }
                    }
                }
            )
        }

        // Auth flow
        composable(authRoute) {
            AuthRoute(
                onAuthSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Tour Detail
        composable(
            route = tourDetailRouteWithArgs,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            TourDetailRoute(
                tourId = tourId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLiveTour = {
                    navController.navigate("$liveTourRoute/$tourId") {
                        popUpTo(tourDetailRoute) { inclusive = true }
                    }
                },
                onNavigateToBooking = {
                    // For now, just show the tour detail with joined state
                    // In the future, this could navigate to a booking confirmation screen
                }
            )
        }

        // Tour Creation/Editing
        composable(
            route = tourCreationRouteWithArgs,
            arguments = listOf(
                navArgument("tourId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val editingTourId = backStackEntry.arguments?.getString("tourId")
            TourCreationRoute(
                onNavigateBack = { navController.popBackStack() },
                onTourCreated = { tourId ->
                    // Navigate to the newly created tour detail
                    navController.navigate("$tourDetailRoute/$tourId") {
                        popUpTo(exploreRoute) // Go back to explore after creation
                    }
                }
            )
        }

        // Live Tour Experience
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
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tour Summary & Feedback
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