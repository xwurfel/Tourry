package com.xwurfel.tourry.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.xwurfel.tourry.ui.discovery.components.TourCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TourDiscoveryScreen(
    onNavigateToTourDetails: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TourDiscoveryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            // TODO: Get actual location
            // In a real app, you'd get the actual location from a location client
            // For simplicity, we'll use a fixed location for this example
            viewModel.loadNearbyTours(latitude = 49.8419, longitude = 24.0315) // Lviv coordinates
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        viewModel.searchTours(searchQuery)
                        showSearchBar = false
                    },
                    active = true,
                    onActiveChange = { showSearchBar = it },
                    placeholder = { Text("Search tours...") },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search suggestions or history could go here
                }
            } else {
                TopAppBar(
                    title = { Text("Discover Tours") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* Navigate to bookmarks screen */ }) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        if (state.isLoading && state.featuredTours.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null && state.featuredTours.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Oops! Something went wrong",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { viewModel.refresh() }) {
                        Text("Try Again")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Featured tours section
                if (state.featuredTours.isNotEmpty()) {
                    item {
                        Text(
                            text = "Featured Tours",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.featuredTours) { tour ->
                                TourCard(
                                    tour = tour,
                                    onClick = { onNavigateToTourDetails(tour.id) },
                                    onBookmarkClick = {
                                        viewModel.toggleBookmark(
                                            tour.id,
                                            tour.isBookmarked
                                        )
                                    },
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Nearby tours section
                if (state.nearbyTours.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tours Near You",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.nearbyTours) { tour ->
                                TourCard(
                                    tour = tour,
                                    onClick = { onNavigateToTourDetails(tour.id) },
                                    onBookmarkClick = {
                                        viewModel.toggleBookmark(
                                            tour.id,
                                            tour.isBookmarked
                                        )
                                    },
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else if (locationPermissionState.status.shouldShowRationale || !locationPermissionState.status.isGranted) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Find Tours Near You",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Allow location access to discover tours in your area",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                                    Text("Enable Location")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Popular tours section
                if (state.popularTours.isNotEmpty()) {
                    item {
                        Text(
                            text = "Popular Tours",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(state.popularTours) { tour ->
                        TourCard(
                            tour = tour,
                            onClick = { onNavigateToTourDetails(tour.id) },
                            onBookmarkClick = {
                                viewModel.toggleBookmark(
                                    tour.id,
                                    tour.isBookmarked
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}