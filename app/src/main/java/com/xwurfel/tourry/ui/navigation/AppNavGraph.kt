package com.xwurfel.tourry.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xwurfel.tourry.ui.auth.LoginScreen
import com.xwurfel.tourry.ui.auth.RegisterScreen
import com.xwurfel.tourry.ui.discovery.TourDetailsScreen
import com.xwurfel.tourry.ui.tourbuilder.SavedToursScreen
import com.xwurfel.tourry.ui.tourbuilder.TourBuilderScreen
import com.xwurfel.tourry.ui.tracking.GroupTrackingScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.TOUR_BUILDER_CREATE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                onRegisterSuccess = {
                    navController.navigate(Routes.TOUR_BUILDER_CREATE) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TOUR_DETAILS,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            TourDetailsScreen(
                tourId = tourId,
                onNavigateBack = { navController.popBackStack() },
                onStartTour = {
                    navController.navigate(
                        Routes.TOUR_TRACKING.replace(
                            "{groupId}",
                            it
                        )
                    )
                }
            )
        }

        composable(Routes.TOUR_BUILDER_CREATE) {
            TourBuilderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTourDetails = { tourId ->
                    navController.navigate(Routes.TOUR_DETAILS.replace("{tourId}", tourId)) {
                        popUpTo(Routes.TOUR_BUILDER_CREATE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TOUR_BUILDER_EDIT,
            arguments = listOf(navArgument("tourId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getString("tourId") ?: ""
            TourBuilderScreen(
                tourId = tourId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTourDetails = { id ->
                    navController.navigate(Routes.TOUR_DETAILS.replace("{tourId}", id)) {
                        popUpTo(Routes.TOUR_BUILDER_EDIT) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TOUR_BUILDER_SAVED) {
            SavedToursScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTourBuilder = { tourId ->
                    navController.navigate(Routes.TOUR_BUILDER_EDIT.replace("{tourId}", tourId))
                },
                onNavigateToNewTour = {
                    navController.navigate(Routes.TOUR_BUILDER_CREATE)
                }
            )
        }

        composable(
            route = Routes.TOUR_TRACKING,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupTrackingScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}