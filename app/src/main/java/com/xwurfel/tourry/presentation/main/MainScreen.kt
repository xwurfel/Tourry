package com.xwurfel.tourry.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xwurfel.tourry.presentation.auth.login.LoginScreenRoute
import com.xwurfel.tourry.presentation.auth.register.RegisterScreenRoute
import com.xwurfel.tourry.presentation.booking.BookingScreenRoute
import com.xwurfel.tourry.presentation.bookings.MyBookingsScreenRoute
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import com.xwurfel.tourry.presentation.common.TourryBottomNavigation
import com.xwurfel.tourry.presentation.navigation.Destinations
import com.xwurfel.tourry.presentation.poi_details.PoiDetailsScreenRoute
import com.xwurfel.tourry.presentation.profile.ProfileScreenRoute
import com.xwurfel.tourry.presentation.save_poi.PoiSettingsScreenRoute
import com.xwurfel.tourry.presentation.tour.checkin.TourCheckInScreenRoute
import com.xwurfel.tourry.presentation.tour.details.TourDetailsScreenRoute
import com.xwurfel.tourry.presentation.tour.edit.EditTourScreenRoute
import com.xwurfel.tourry.presentation.tour.list.TourListScreenRoute
import com.xwurfel.tourry.presentation.tour.route.RouteEditorScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (uiState.isInitializing) {
        LoadingScreenContent()
        LaunchedEffect(Unit) {
            viewModel.checkAuthState()
        }
        return
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated && currentRoute != "com.xwurfel.tourry.presentation.navigation.Destinations.Login" && currentRoute != "com.xwurfel.tourry.presentation.navigation.Destinations.Register") {
            navController.navigate(Destinations.Login) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    // TODO: remove hardcoded strings from code
    val currentDestination: Destinations = try {
        when (currentRoute) {
            "com.xwurfel.tourry.presentation.navigation.Destinations.TourList" -> Destinations.TourList
            "com.xwurfel.tourry.presentation.navigation.Destinations.MyBookings" -> Destinations.MyBookings
            "com.xwurfel.tourry.presentation.navigation.Destinations.Profile" -> Destinations.Profile
            else -> Destinations.TourList
        }
    } catch (_: Exception) {
        Destinations.TourList
    }

    val showBottomNav = when (currentDestination) {
        is Destinations.TourList, is Destinations.MyBookings, is Destinations.Profile -> true

        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav && uiState.isAuthenticated) {
                TourryBottomNavigation(
                    currentRoute = currentDestination, onItemSelected = { destination ->
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
                    })
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.isAuthenticated) Destinations.TourList else Destinations.Login,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth screens
            composable<Destinations.Login> {
                LoginScreenRoute(onNavigateToRegister = {
                    navController.navigate(Destinations.Register)
                }, onLoginSuccess = {
                    viewModel.setAuthenticated(true)
                    navController.navigate(Destinations.TourList) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                })
            }

            composable<Destinations.Register> {
                RegisterScreenRoute(onNavigateBack = {
                    navController.navigateUp()
                }, onRegisterSuccess = {
                    viewModel.setAuthenticated(true)
                    navController.navigate(Destinations.TourList) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                })
            }

            // Main application screens
            composable<Destinations.TourList> {
                TourListScreenRoute(onTourClicked = { tourId ->
                    navController.navigate(Destinations.TourDetails(tourId))
                }, onCreateTourClicked = {
                    navController.navigate(Destinations.EditTour())
                })
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
                    },
                    onEditRouteClicked = { id ->
                        navController.navigate(Destinations.TourRouteEditor(id))
                    },
                    onCheckInClicked = { id ->
                        navController.navigate(Destinations.TourCheckIn(id))
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
                    })
            }

            composable<Destinations.Profile> {
                ProfileScreenRoute(onNavigateToLogin = {
                    viewModel.setAuthenticated(false)
                    navController.navigate(Destinations.Login) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }, onNavigateToMyTours = {
                    // TODO: implement
                    // Navigate to My Tours screen (for guides)
                    // This is a placeholder for now
                }, onNavigateToMyBookings = {
                    navController.navigate(Destinations.MyBookings)
                })
            }

            composable<Destinations.PoiSettings> {
                val latitude = it.toRoute<Destinations.PoiSettings>().latitude
                val longitude = it.toRoute<Destinations.PoiSettings>().longitude
                PoiSettingsScreenRoute(
                    latitude = latitude, longitude = longitude, onBack = navController::navigateUp
                )
            }

            composable<Destinations.PoiDetails> {
                val poiId = it.toRoute<Destinations.PoiDetails>().poiId
                PoiDetailsScreenRoute(
                    poiId = poiId, onBack = navController::navigateUp
                )
            }

            composable<Destinations.MyBookings> {
                MyBookingsScreenRoute(
                    onNavigateToTourDetails = { tourId ->
                        navController.navigate(Destinations.TourDetails(tourId))
                    })
            }

            composable<Destinations.EditTour> {
                val tourId = it.toRoute<Destinations.EditTour>().tourId
                EditTourScreenRoute(
                    tourId = tourId, onNavigateBack = navController::navigateUp
                )
            }

            composable<Destinations.EditTour> {
                val tourId = it.toRoute<Destinations.EditTour>().tourId
                EditTourScreenRoute(
                    tourId = tourId, onNavigateBack = navController::navigateUp
                )
            }

            composable<Destinations.TourRouteEditor> {
                val tourId = it.toRoute<Destinations.TourRouteEditor>().tourId
                RouteEditorScreenRoute(
                    tourId = tourId,
                    onNavigateBack = navController::navigateUp
                )
            }

            composable<Destinations.TourCheckIn> {
                val tourId = it.toRoute<Destinations.TourCheckIn>().tourId
                TourCheckInScreenRoute(
                    tourId = tourId,
                    onNavigateBack = navController::navigateUp
                )
            }
        }
    }
}