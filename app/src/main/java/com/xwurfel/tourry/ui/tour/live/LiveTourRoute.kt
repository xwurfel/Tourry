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
import androidx.compose.material.icons.filled.LocationOff
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.feature.audio.domain.model.AudioPlayerState
import com.xwurfel.tourry.feature.location.domain.model.UserLocation
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import com.xwurfel.tourry.feature.tours.domain.model.RouteDeviation
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import com.xwurfel.tourry.feature.tours.domain.model.TourStatus
import com.xwurfel.tourry.ui.theme.TourryTheme
import com.xwurfel.tourry.util.permissions.LocationPermissionsHandler

@Composable
fun LiveTourRoute(
    tourId: String,
    onTourCompleted: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LiveTourViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            LiveTourEvent.TourCompleted -> onTourCompleted()
            LiveTourEvent.NavigateBack -> onNavigateBack()
        }
    }

    if (!hasRequestedPermissions) {
        LocationPermissionsHandler(
            onPermissionsGranted = {
                hasRequestedPermissions = true
                viewModel.acceptIntent(LiveTourIntent.StartLocationTracking)
            }
        )
    }

    LiveTourScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTourScreen(
    uiState: LiveTourUiState,
    onIntent: (LiveTourIntent) -> Unit,
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
            TourActionFab(
                uiState = uiState,
                onIntent = onIntent
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error,
                        onRetry = { onIntent(LiveTourIntent.StartLocationTracking) }
                    )
                }

                uiState.tourStops.isNotEmpty() -> {
                    LiveTourMapView(uiState = uiState)
                }
            }

            // Overlays
            TourProgressOverlay(
                progress = uiState.progress,
                currentStop = uiState.currentStopIndex + 1,
                totalStops = uiState.tourStops.size,
                modifier = Modifier.align(Alignment.TopStart)
            )

            TourStatusIndicator(
                status = uiState.tourStatus,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Current stop content card
            uiState.currentStop?.let { stop ->
                if (stop.isActive && stop.content != null) {
                    StopContentCard(
                        content = stop.content,
                        stopName = stop.name,
                        stopOrder = stop.order,
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
        }
    }

    // Dialogs
    if (showExitDialog) {
        ExitTourDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onIntent(LiveTourIntent.ExitTour)
            }
        )
    }

    if (uiState.showRouteDeviationWarning && uiState.routeDeviation != null) {
        RouteDeviationDialog(
            deviation = uiState.routeDeviation,
            onDismiss = { onIntent(LiveTourIntent.DismissRouteDeviation) }
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Preparing your tour...",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Getting location and loading tour data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    "Tour Setup Failed",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun LiveTourMapView(uiState: LiveTourUiState) {
    if (uiState.userLocation != null && uiState.tourStops.isNotEmpty()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                17f
            )
        }

        LaunchedEffect(uiState.userLocation) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                    17f
                )
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true
            )
        ) {
            // User location marker
            Marker(
                state = rememberMarkerState(
                    key = uiState.userLocation.toString(),
                    position = LatLng(
                        uiState.userLocation.latitude,
                        uiState.userLocation.longitude
                    )
                ),
                title = "You are here"
            )

            // Tour stops and geofences
            uiState.tourStops.forEach { stop ->
                val stopColor = when {
                    stop.isActive -> MaterialTheme.colorScheme.primary
                    stop.isVisited -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }

                // Geofence circle
                Circle(
                    center = LatLng(stop.latitude, stop.longitude),
                    radius = stop.geofenceRadius.toDouble(),
                    fillColor = stopColor.copy(alpha = 0.2f),
                    strokeColor = stopColor,
                    strokeWidth = 2f
                )

                // Stop marker
                Marker(
                    state = MarkerState(
                        position = LatLng(stop.latitude, stop.longitude)
                    ),
                    title = "${stop.order}. ${stop.name}"
                )
            }

            // Route line connecting stops
            if (uiState.tourStops.size > 1) {
                Polyline(
                    points = uiState.tourStops.map {
                        LatLng(it.latitude, it.longitude)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    width = 5f
                )
            }
        }
    } else {
        // Waiting for location
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Waiting for location...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun TourActionFab(
    uiState: LiveTourUiState,
    onIntent: (LiveTourIntent) -> Unit
) {
    when (uiState.tourStatus) {
        TourStatus.PREPARING -> {
            // No FAB during preparation
        }

        TourStatus.ACTIVE -> {
            if (uiState.canComplete) {
                FloatingActionButton(
                    onClick = { onIntent(LiveTourIntent.CompleteTour) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete tour")
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

        TourStatus.PAUSED -> {
            FloatingActionButton(
                onClick = { onIntent(LiveTourIntent.ResumeTour) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume tour")
            }
        }

        TourStatus.COMPLETED -> {
            // Tour completed, no FAB needed
        }
    }
}

@Composable
private fun TourProgressOverlay(
    progress: Float,
    currentStop: Int,
    totalStops: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Stop $currentStop of $totalStops",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = { progress },
                drawStopIndicator = {},
                modifier = Modifier.width(120.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TourStatusIndicator(
    status: TourStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = when (status) {
            TourStatus.PREPARING -> MaterialTheme.colorScheme.surfaceVariant
            TourStatus.ACTIVE -> MaterialTheme.colorScheme.primary
            TourStatus.PAUSED -> MaterialTheme.colorScheme.secondary
            TourStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        }
    ) {
        Text(
            text = when (status) {
                TourStatus.PREPARING -> "PREPARING"
                TourStatus.ACTIVE -> "LIVE"
                TourStatus.PAUSED -> "PAUSED"
                TourStatus.COMPLETED -> "COMPLETED"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                TourStatus.PREPARING -> MaterialTheme.colorScheme.onSurfaceVariant
                TourStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                TourStatus.PAUSED -> MaterialTheme.colorScheme.onSecondary
                TourStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiary
            }
        )
    }
}

@Composable
fun StopContentCard(
    content: StopContent,
    stopName: String,
    stopOrder: Int,
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
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stopOrder.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
            if (content.imageUrls.isNotEmpty()) {
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
                    text = content.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Audio Player
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
    val isPlaying = isCurrentlyPlaying && audioPlayerState?.isPlaying == true
    val isLoading = isCurrentlyPlaying && audioPlayerState?.isLoading == true

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
            // Player controls
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
                    }
                ) {
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

            // Progress bar
            if (isCurrentlyPlaying && audioPlayerState != null && audioPlayerState.duration > 0) {
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

            // Error state
            if (isCurrentlyPlaying && audioPlayerState?.hasError == true) {
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

@Composable
private fun ExitTourDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Tour?") },
        text = {
            Text("Are you sure you want to exit the tour? Your progress will be saved.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Exit Tour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue Tour")
            }
        }
    )
}

@Composable
fun RouteDeviationDialog(
    deviation: RouteDeviation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "You've wandered off the route",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You're about ${deviation.distanceFromRoute.toInt()}m away from the tour route.",
                    style = MaterialTheme.typography.bodyMedium
                )

                deviation.nearestStopName?.let { stopName ->
                    Text(
                        "Head towards: $stopName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    "Follow the route to continue your tour experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Got it")
            }
        }
    )
}

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

@Preview
@Composable
private fun LiveTourPreview() {
    TourryTheme {
        LiveTourScreen(
            LiveTourUiState(
                tourStops = listOf(
                    LiveTourStop(
                        id = "1",
                        name = "Courtney Stein",
                        description = "sed",
                        latitude = 6.7,
                        longitude = 8.9,
                        order = 4364,
                        geofenceRadius = 10.11f,
                        content = null,
                        isVisited = false,
                        isActive = false
                    ),
                    LiveTourStop(
                        id = "2",
                        name = "Courtney Stein 2",
                        description = "sed",
                        latitude = 6.75,
                        longitude = 8.95,
                        order = 4365,
                        geofenceRadius = 10.11f,
                        content = null,
                        isVisited = false,
                        isActive = false
                    )
                ),
                userLocation = UserLocation(60.0, 60.0)
            ),
            onIntent = {}
        )
    }
}