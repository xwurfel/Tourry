package com.xwurfel.tourry.presentation.home

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.presentation.navigation.Destinations
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import com.xwurfel.tourry.presentation.home.model.PoiClusterItem

@Composable
fun HomeScreenRoute(onNavigate: (Destinations) -> Unit) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            LoadingScreenContent(Modifier.fillMaxSize())
        }

        uiState.errorMessage != null -> {
            ErrorScreenContent(uiState.errorMessage ?: "", Modifier.fillMaxSize())
        }

        else -> {
            HomeScreen(
                uiState = uiState,
                onNavigate = {
                    onNavigate(
                        Destinations.PoiSettings(
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    )
                },
                onMapLoaded = viewModel::onMapLoaded,
                onPermissionGranted = viewModel::onPermissionGranted,
                onNavigateToPoiDetails = { poiId ->
                    onNavigate(Destinations.PoiDetails(poiId))
                }
            )
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class, MapsComposeExperimentalApi::class)
@Composable
fun HomeScreen(
    uiState: HomeViewModel.HomeScreenUiState,
    onMapLoaded: () -> Unit = {},
    onNavigate: (LatLng) -> Unit = {},
    onPermissionGranted: () -> Unit = {},
    onNavigateToPoiDetails: (Long) -> Unit = {},
) {
    val multiplePermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    val cameraPositionState = rememberCameraPositionState()

    val mapProperties = remember(uiState.isLocationPermissionGranted) {
        MapProperties(
            isMyLocationEnabled = uiState.isLocationPermissionGranted,
            mapType = MapType.HYBRID
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            onMapLoaded = onMapLoaded,
            onMapLongClick = onNavigate,
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
        ) {
            Clustering(
                items = uiState.pois.map { PoiClusterItem(it) },
                onClusterItemClick = { clusterItem ->
                    onNavigateToPoiDetails(clusterItem.poi.id)
                    false
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        multiplePermissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(multiplePermissionState.allPermissionsGranted) {
        if (multiplePermissionState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(uiState.userLocation) {
        if (uiState.userLocation != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude), 15f
                )
            )
        }
    }
}

@Preview
@Composable
private fun PreviewHomeScreen() {
    HomeScreen(uiState = HomeViewModel.HomeScreenUiState())
}