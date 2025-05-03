package com.xwurfel.tourry.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xwurfel.tourry.presentation.navigation.Destinations
import com.xwurfel.tourry.presentation.booking.BookingScreenRoute
import com.xwurfel.tourry.presentation.common.TourryBottomNavigation
import com.xwurfel.tourry.presentation.home.HomeScreenRoute
import com.xwurfel.tourry.presentation.poi_details.PoiDetailsScreenRoute
import com.xwurfel.tourry.presentation.save_poi.PoiSettingsScreenRoute
import com.xwurfel.tourry.presentation.tour.details.TourDetailsScreenRoute
import com.xwurfel.tourry.presentation.tour.list.TourListScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine current destination (needed for bottom navigation)
    val currentDestination: Destinations = try {
        when (currentRoute) {
            "TourList" -> Destinations.TourList
            "MyBookings" -> Destinations.MyBookings
            "Profile" -> Destinations.Profile
            else -> Destinations.TourList
        }
    } catch (_: Exception) {
        Destinations.TourList
    }

    // Check if we should show bottom navigation (hide it on detail screens)
    val showBottomNav = when (currentDestination) {
        is Destinations.TourList,
        is Destinations.MyBookings,
        is Destinations.Profile -> true

        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                TourryBottomNavigation(
                    currentRoute = currentDestination,
                    onItemSelected = { destination ->
                        navController.navigate(destination) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.TourList,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Destinations.TourList> {
                TourListScreenRoute(
                    onTourClicked = { tourId ->
                        navController.navigate(Destinations.TourDetails(tourId))
                    },
                    onCreateTourClicked = {
                        navController.navigate(Destinations.EditTour())
                    }
                )
            }

            composable<Destinations.TourDetails> {
                val tourId = it.toRoute<Destinations.TourDetails>().tourId
                TourDetailsScreenRoute(
                    tourId = tourId,
                    onBackClicked = navController::navigateUp,
                    onEditClicked = { id ->
                        navController.navigate(Destinations.EditTour(id))
                    },
                    onBookNowClicked = { id ->
                        navController.navigate(Destinations.BookTour(id))
                    }
                )
            }

            composable<Destinations.BookTour> {
                val tourId = it.toRoute<Destinations.BookTour>().tourId
                BookingScreenRoute(
                    tourId = tourId,
                    onBackClicked = navController::navigateUp,
                    onBookingComplete = {
                        // Navigate back to tour details
                        navController.navigateUp()
                    }
                )
            }

            // Original POI screens
            composable<Destinations.Home> {
                HomeScreenRoute(
                    onNavigate = {
                        navController.navigate(it)
                    }
                )
            }

            composable<Destinations.PoiSettings> {
                val latitude = it.toRoute<Destinations.PoiSettings>().latitude
                val longitude = it.toRoute<Destinations.PoiSettings>().longitude
                PoiSettingsScreenRoute(
                    latitude = latitude,
                    longitude = longitude,
                    onBack = navController::navigateUp
                )
            }

            composable<Destinations.PoiDetails> {
                val poiId = it.toRoute<Destinations.PoiDetails>().poiId
                PoiDetailsScreenRoute(
                    poiId = poiId,
                    onBack = navController::navigateUp
                )
            }

            // TODO: Add placeholder composables for remaining screens
            composable<Destinations.MyBookings> {
                // Placeholder for MyBookings screen
                // Will be implemented later
            }

            composable<Destinations.Profile> {
                // Placeholder for Profile screen
                // Will be implemented later
            }

            composable<Destinations.EditTour> {
                // Placeholder for EditTour screen
                // Will be implemented later
            }

            composable<Destinations.Login> {
                // Placeholder for Login screen
                // Will be implemented later
            }

            composable<Destinations.Register> {
                // Placeholder for Register screen
                // Will be implemented later
            }
        }
    }
}