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
                    navController.navigate(TourryNavigation.createTourDetailRoute(tourId))
                },
                onNavigateToTourCreation = {
                    navController.navigate(tourCreationRoute)
                }
            )
        }

        composable(myToursRoute) {
            MyToursRoute(
                onNavigateToTourDetail = { tourId ->
                    navController.navigate(TourryNavigation.createTourDetailRoute(tourId))
                },
                onNavigateToLiveTour = { tourId ->
                    navController.navigate(TourryNavigation.createLiveTourRoute(tourId))
                },
                onNavigateToTourSummary = { tourId ->
                    navController.navigate(TourryNavigation.createTourSummaryRoute(tourId))
                },
                onNavigateToTourEdit = { tourId ->
                    navController.navigate(TourryNavigation.createTourCreationRoute(tourId))
                }
            )
        }

        composable(profileRoute) {
            ProfileRoute(
                onNavigateToCreateTour = {
                    navController.navigate(tourCreationRoute)
                },
                onNavigateToMyTours = {
                    navController.navigate(myToursRoute) {
                        // Navigate to MyTours tab but don't clear profile from backstack
                        launchSingleTop = true
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(authRoute)
                }
            )
        }

        composable(authRoute) {
            AuthRoute(
                onAuthSuccess = {
                    if (!navController.popBackStack()) {
                        navController.navigate(exploreRoute) {
                            popUpTo(authRoute) { inclusive = true }
                        }
                    }
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
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLiveTour = {
                    navController.navigate(TourryNavigation.createLiveTourRoute(tourId)) {
                        // Clear the tour detail from backstack since we're starting the live tour
                        popUpTo(tourDetailRouteWithArgs) { inclusive = true }
                    }
                },
                onNavigateToBooking = {
                    // For now, just stay on tour detail with updated joined state
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
            // TODO: add editingTourId
            TourCreationRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTourCreated = { tourId ->
                    navController.navigate(TourryNavigation.createTourDetailRoute(tourId)) {
                        popUpTo(tourCreationRouteWithArgs) { inclusive = true }
                    }
                }
            )
        }

        // Simple tour creation without arguments
        composable(tourCreationRoute) {
            TourCreationRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTourCreated = { tourId ->
                    // Navigate to the newly created tour detail and clear creation from backstack
                    navController.navigate(TourryNavigation.createTourDetailRoute(tourId)) {
                        popUpTo(tourCreationRoute) { inclusive = true }
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
                    // Navigate to summary and clear live tour from backstack
                    navController.navigate(TourryNavigation.createTourSummaryRoute(tourId)) {
                        popUpTo(liveTourRouteWithArgs) { inclusive = true }
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
                    // Navigate to explore and clear everything above it from backstack
                    navController.navigate(exploreRoute) {
                        popUpTo(exploreRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}