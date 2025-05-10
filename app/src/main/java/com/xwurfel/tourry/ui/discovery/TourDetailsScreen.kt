package com.xwurfel.tourry.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourDetailsScreen(
    tourId: String,
    onNavigateBack: () -> Unit,
    onStartTour: (String) -> Unit,
    viewModel: TourDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(tourId) {
        viewModel.loadTourDetails(tourId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Empty title, shown in content */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val tourDetails = state.tourDetails
                    if (tourDetails != null) {
                        IconButton(onClick = { viewModel.toggleBookmark() }) {
                            Icon(
                                imageVector = if (tourDetails.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (tourDetails.isBookmarked) "Remove bookmark" else "Add bookmark"
                            )
                        }
                    }
                    IconButton(onClick = { /* Share tour */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            val tourDetails = state.tourDetails
            if (tourDetails != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Price info
                        Column {
                            Text(
                                text = tourDetails.price?.let { "$$it" } ?: "Free",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "per person",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Start tour button
                        Button(onClick = { onStartTour(tourId) }) {
                            Text("Start Tour")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
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
                        text = "Couldn't load tour details",
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

                    Button(onClick = { viewModel.loadTourDetails(tourId) }) {
                        Text("Try Again")
                    }
                }
            }
        } else {
            state.tourDetails?.let { tourDetails ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    // Header image
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            AsyncImage(
                                model = tourDetails.thumbnailUrl
                                    ?: tourDetails.imageUrls.firstOrNull()
                                    ?: "https://via.placeholder.com/800x400?text=Tour",
                                contentDescription = tourDetails.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Gradient overlay for better text visibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color.Transparent,
                                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )

                            // Title and basic info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = tourDetails.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = androidx.compose.ui.graphics.Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = tourDetails.location,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Rating
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%.1f",
                                            tourDetails.rating
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = "(${tourDetails.reviewCount} reviews)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Key information
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Duration
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${tourDetails.duration.toHours()}h ${tourDetails.duration.toMinutesParts()}m",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Distance
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = tourDetails.distance?.let {
                                            String.format(
                                                Locale.getDefault(),
                                                "%.1f km",
                                                it
                                            )
                                        } ?: "N/A",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Difficulty
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (tourDetails.difficulty) {
                                            TourDifficulty.EASY -> Icons.AutoMirrored.Filled.DirectionsWalk
                                            TourDifficulty.MODERATE -> Icons.Default.Hiking
                                            TourDifficulty.CHALLENGING -> Icons.Default.Terrain
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = tourDetails.difficulty.name.lowercase()
                                            .capitalize(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Waypoints
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tour,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${tourDetails.waypointCount} stops",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Description
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "About this tour",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tourDetails.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Highlights
                    if (tourDetails.highlights.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Highlights",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                tourDetails.highlights.forEach { highlight ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = highlight,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gallery
                    if (tourDetails.imageUrls.size > 1) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Gallery",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(tourDetails.imageUrls) { imageUrl ->
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 160.dp, height = 120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Creator info
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Creator avatar placeholder
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tourDetails.creatorName.first().toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = "Created by",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = tourDetails.creatorName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    // Add space at the bottom for the bottom bar
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Helper extension
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// Helper extension
private fun java.time.Duration.toMinutesParts(): Int {
    return (this.toMinutes() % 60).toInt()
}