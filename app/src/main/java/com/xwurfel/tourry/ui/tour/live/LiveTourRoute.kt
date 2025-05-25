package com.xwurfel.tourry.ui.tour.live

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.util.permissions.LocationPermissionsHandler

@Composable
fun LiveTourRoute(
    tourId: String,
    onTourCompleted: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LiveTourViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            LiveTourEvent.TourCompleted -> onTourCompleted()
            LiveTourEvent.NavigateBack -> onNavigateBack()
        }
    }

    // Handle location permissions
    LocationPermissionsHandler(
        onPermissionsGranted = {
            viewModel.acceptIntent(LiveTourIntent.StartLocationTracking)
        }
    )

    LiveTourScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTourScreen(
    uiState: LiveTourUiState,
    onIntent: (LiveTourIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.tourTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit tour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (uiState.tourStatus == TourStatus.COMPLETED) {
                FloatingActionButton(
                    onClick = { onIntent(LiveTourIntent.CompleteTour) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete tour")
                }
            } else if (uiState.tourStatus == TourStatus.PAUSED) {
                FloatingActionButton(
                    onClick = { onIntent(LiveTourIntent.ResumeTour) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume tour")
                }
            } else {
                FloatingActionButton(
                    onClick = { onIntent(LiveTourIntent.PauseTour) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause tour")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map
            if (uiState.userLocation != null && uiState.tourStops.isNotEmpty()) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                        17f
                    )
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    Marker(
                        state = remember(uiState.userLocation) {
                            MarkerState(
                                position = LatLng(
                                    uiState.userLocation.latitude,
                                    uiState.userLocation.longitude
                                )
                            )
                        },
                        title = "You are here"
                    )

                    // Tour stops and geofences
                    uiState.tourStops.forEachIndexed { index, stop ->
                        val isCompleted = index < uiState.currentStopIndex
                        val isCurrent = index == uiState.currentStopIndex
                        val isActive = stop.isActive

                        // Geofence circle
                        Circle(
                            center = LatLng(stop.latitude, stop.longitude),
                            radius = stop.geofenceRadius.toDouble(),
                            fillColor = if (isActive)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else if (isCompleted)
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            strokeColor = if (isActive)
                                MaterialTheme.colorScheme.primary
                            else if (isCompleted)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.outline,
                            strokeWidth = 2f
                        )

                        // Stop marker
                        Marker(
                            state = MarkerState(
                                position = LatLng(stop.latitude, stop.longitude)
                            ),
                            title = "${index + 1}. ${stop.name}"
                        )
                    }
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Current stop content card
            uiState.currentStop?.let { stop ->
                if (stop.isActive && stop.content != null) {
                    StopContentCard(
                        content = stop.content,
                        stopName = stop.name,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        onDismiss = { onIntent(LiveTourIntent.DismissContent) }
                    )
                }
            }

            // Status indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = when (uiState.tourStatus) {
                    TourStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                    TourStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                    TourStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                }
            ) {
                Text(
                    text = uiState.tourStatus.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (uiState.tourStatus) {
                        TourStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                        TourStatus.PAUSED -> MaterialTheme.colorScheme.onSecondary
                        TourStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiary
                    }
                )
            }

            // Stop counter
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${uiState.currentStopIndex + 1} / ${uiState.tourStops.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Tour?") },
            text = {
                Text("Are you sure you want to exit the tour? Your progress will be saved.")
            },
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    onIntent(LiveTourIntent.ExitTour)
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StopContentCard(
    content: StopContent,
    stopName: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            // Content
            if (content.imageUrl != null) {
                AsyncImage(
                    model = content.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (content.text.isNotEmpty()) {
                Text(
                    text = content.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Audio player
            if (content.audioUrl != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { /* TODO: Play/pause audio */ }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play audio",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Audio Guide",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Tap to listen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}