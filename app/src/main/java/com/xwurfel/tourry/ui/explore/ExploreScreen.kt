package com.xwurfel.tourry.ui.explore

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.feature.tours.domain.model.TourPreview
import com.xwurfel.tourry.ui.explore.components.ExploreFiltersBar
import com.xwurfel.tourry.ui.explore.components.SearchBar
import com.xwurfel.tourry.ui.explore.components.TourCard
import com.xwurfel.tourry.ui.main.LocalSnackbarHostState

@Composable
fun ExploreRoute(
    onNavigateToTourDetail: (String) -> Unit,
    onNavigateToTourCreation: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            is ExploreEvent.NavigateToTourDetail -> onNavigateToTourDetail(event.tourId)
            ExploreEvent.NavigateToTourCreation -> onNavigateToTourCreation()
        }
    }

    // Show snackbar for errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    ExploreScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onIntent: (ExploreIntent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_explore)) },
                actions = {
                    IconButton(onClick = { onIntent(ExploreIntent.ToggleViewMode) }) {
                        Icon(
                            imageVector = if (uiState.isMapMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.Map,
                            contentDescription = stringResource(
                                if (uiState.isMapMode) R.string.switch_to_list else R.string.switch_to_map
                            )
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onIntent(ExploreIntent.CreateTourClicked) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.create_tour)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onIntent(ExploreIntent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filters
            ExploreFiltersBar(
                filters = uiState.activeFilters,
                onFiltersChanged = { onIntent(ExploreIntent.FilterChanged(it)) },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isMapMode) {
                    // Map View
                    TourMapView(
                        uiState.userLocation,
                        tours = uiState.tours,
                        onTourClick = { tourId -> onIntent(ExploreIntent.TourClicked(tourId)) }
                    )
                } else {
                    // List View
                    TourListView(
                        tours = uiState.tours,
                        joinedTourIds = uiState.joinedTourIds,
                        isLoading = uiState.isLoading,
                        onTourClick = { tourId -> onIntent(ExploreIntent.TourClicked(tourId)) },
                        onJoinTour = { tourId -> onIntent(ExploreIntent.JoinTour(tourId)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TourMapView(
    userLocation: Location?,
    tours: List<TourPreview>,
    onTourClick: (String) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation?.let {
                LatLng(userLocation.latitude, userLocation.longitude)
            } ?: LatLng(48.8566, 2.3522),
            12f
        )
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp)),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true
        )
    ) {
        tours.forEach { tour ->
            Marker(
                state = MarkerState(
                    position = tour.coordinates
                ),
                title = tour.title,
                snippet = if (tour.isFree) "Free" else "$${tour.price.toInt()}",
                onInfoWindowClick = { onTourClick(tour.id) }
            )
        }
    }
}

@Composable
fun TourListView(
    tours: List<TourPreview>,
    joinedTourIds: Set<String>,
    isLoading: Boolean,
    onTourClick: (String) -> Unit,
    onJoinTour: (String) -> Unit
) {
    when {
        isLoading && tours.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        tours.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No tours found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try adjusting your search or filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tours, key = { it.id }) { tour ->
                    TourCard(
                        tour = tour,
                        onClick = { onTourClick(tour.id) },
                        onJoinClick = onJoinTour,
                        isJoined = tour.id in joinedTourIds,
                        isJoining = false // TODO: Add joining state per tour
                    )
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}