package com.xwurfel.tourry.ui.tour.live

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.feature.audio.PlaybackState
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
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
        })

    LiveTourScreen(
        uiState = uiState, onIntent = viewModel::acceptIntent, onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTourScreen(
    uiState: LiveTourUiState, onIntent: (LiveTourIntent) -> Unit, onNavigateBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    uiState.tourTitle, style = MaterialTheme.typography.titleMedium
                )
            }, navigationIcon = {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit tour")
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }, floatingActionButton = {
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
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map
            if (uiState.userLocation != null && uiState.tourStops.isNotEmpty()) {
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude), 17f
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
                                    uiState.userLocation.latitude, uiState.userLocation.longitude
                                )
                            )
                        }, title = "You are here"
                    )

                    uiState.tourStops.forEachIndexed { index, stop ->
                        val isCompleted = index < uiState.currentStopIndex
                        val isCurrent = index == uiState.currentStopIndex
                        val isActive = stop.isActive

                        // Geofence circle
                        Circle(
                            center = LatLng(stop.latitude, stop.longitude),
                            radius = stop.geofenceRadius.toDouble(),
                            fillColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else if (isCompleted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            strokeColor = if (isActive) MaterialTheme.colorScheme.primary
                            else if (isCompleted) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outline,
                            strokeWidth = 2f
                        )

                        // Stop marker
                        Marker(
                            state = MarkerState(
                                position = LatLng(stop.latitude, stop.longitude)
                            ), title = "${index + 1}. ${stop.name}"
                        )
                    }

                    if (uiState.routeDeviation?.isDeviated == true) {
                        uiState.routeDeviation.nearestStopName?.let { stopName ->
                            val nearestStop = uiState.tourStops.find { it.name == stopName }
                            nearestStop?.let { stop ->
                                Polyline(
                                    points = listOf(
                                        LatLng(
                                            uiState.userLocation.latitude,
                                            uiState.userLocation.longitude
                                        ), LatLng(stop.latitude, stop.longitude)
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    width = 8f,
                                    pattern = listOf(
                                        Dash(20f), Gap(10f)
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
                        audioPlayerState = uiState.audioPlayerState,
                        currentlyPlayingAudio = uiState.currentlyPlayingAudio,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        onDismiss = { onIntent(LiveTourIntent.DismissContent) },
                        onPlayAudio = { audioUrl -> onIntent(LiveTourIntent.PlayAudio(audioUrl)) },
                        onPauseAudio = { onIntent(LiveTourIntent.PauseAudio) },
                        onResumeAudio = { onIntent(LiveTourIntent.ResumeAudio) },
                        onSeekAudio = { position -> onIntent(LiveTourIntent.SeekAudio(position)) }
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
            })
    }

    if (uiState.showRouteDeviationWarning && uiState.routeDeviation != null) {
        RouteDeviationDialog(
            deviation = uiState.routeDeviation,
            onDismiss = { onIntent(LiveTourIntent.DismissRouteDeviation) },
            onNavigateBack = {
                onIntent(LiveTourIntent.DismissRouteDeviation)
            })
    }
}

@Composable
fun StopContentCard(
    content: StopContent,
    stopName: String,
    audioPlayerState: AudioPlayerState?,
    currentlyPlayingAudio: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header (unchanged)
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

            // Content (unchanged)
            if (content.imageUrls.isNotEmpty()) {
                // TODO: Add support for multiple images
                AsyncImage(
                    model = content.imageUrls.first(),
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
                    text = content.text, style = MaterialTheme.typography.bodyMedium
                )
            }

            // Enhanced Audio Player
            if (content.audioUrl != null) {
                AudioPlayerCard(
                    audioUrl = content.audioUrl,
                    audioPlayerState = audioPlayerState,
                    isCurrentlyPlaying = currentlyPlayingAudio == content.audioUrl,
                    onPlayAudio = onPlayAudio,
                    onPauseAudio = onPauseAudio,
                    onResumeAudio = onResumeAudio,
                    onSeekAudio = onSeekAudio
                )
            }
        }
    }
}


@Composable
fun AudioPlayerCard(
    audioUrl: String,
    audioPlayerState: AudioPlayerState?,
    isCurrentlyPlaying: Boolean,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    val isPlaying = isCurrentlyPlaying && audioPlayerState?.playbackState == PlaybackState.PLAYING
    val isLoading = isCurrentlyPlaying && audioPlayerState?.playbackState == PlaybackState.LOADING

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play/Pause button
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        when {
                            isLoading -> { /* Do nothing while loading */
                            }

                            !isCurrentlyPlaying -> onPlayAudio(audioUrl)
                            isPlaying -> onPauseAudio()
                            else -> onResumeAudio()
                        }
                    }) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Audio Guide",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (isCurrentlyPlaying) "Now playing" else "Tap to listen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Duration display
                if (isCurrentlyPlaying && audioPlayerState != null) {
                    Text(
                        "${formatTime(audioPlayerState.currentPosition)} / ${
                            formatTime(
                                audioPlayerState.duration
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Progress bar (only when playing current audio)
            if (isCurrentlyPlaying && audioPlayerState != null && audioPlayerState.duration > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Slider(
                        value = audioPlayerState.currentPosition.toFloat(),
                        onValueChange = { newPosition ->
                            onSeekAudio(newPosition.toInt())
                        },
                        valueRange = 0f..audioPlayerState.duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Error state
            if (isCurrentlyPlaying && audioPlayerState?.playbackState == PlaybackState.ERROR) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Unable to play audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

@Composable
fun RouteDeviationDialog(
    deviation: RouteDeviation, onDismiss: () -> Unit, onNavigateBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }, title = {
            Text(
                "You've strayed from the route",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You're about ${deviation.distanceFromRoute.toInt()}m away from the tour route.",
                    style = MaterialTheme.typography.bodyMedium
                )

                deviation.nearestStopName?.let { stopName ->
                    Text(
                        "The nearest stop is: $stopName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Please return to the marked route to continue your tour experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }, confirmButton = {
            Button(
                onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guide me back")
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("I'll find my way")
            }
        }, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp
    )
}