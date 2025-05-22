package com.xwurfel.tourry.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle

@Composable
fun ExploreRoute(
    onNavigateToTourDetail: (String) -> Unit,
    onNavigateToTourCreation: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            is ExploreEvent.NavigateToTourDetail -> onNavigateToTourDetail(event.tourId)
            ExploreEvent.NavigateToTourCreation -> onNavigateToTourCreation()
        }
    }

    ExploreScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onIntent: (ExploreIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_explore)) },
                actions = {
                    IconButton(onClick = { onIntent(ExploreIntent.ToggleViewMode) }) {
                        Icon(
                            imageVector = if (uiState.isMapMode) Icons.Default.ViewList else Icons.Default.Map,
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onIntent(ExploreIntent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filters
            ExploreFilters(
                filters = uiState.activeFilters,
                onFiltersChanged = { onIntent(ExploreIntent.FilterChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isMapMode) {
                    // Map View
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = rememberCameraPositionState(),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = true
                        )
                    ) {
                        // TODO: Add tour markers
                    }
                } else {
                    // List View
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.tours) { tour ->
                                TourCard(
                                    tour = tour,
                                    onClick = { onIntent(ExploreIntent.TourClicked(tour.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}