package com.xwurfel.tourry.ui.tour.live

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun LiveTourRoute(
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

    LaunchedEffect(Unit) {
        viewModel.acceptIntent(LiveTourIntent.Start)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTourScreen(
    uiState: LiveTourUiState,
    onIntent: (LiveTourIntent) -> Unit,
    showDebugInfo: Boolean = true
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

            // Debug overlay (only in debug builds)
//            if (showDebugInfo) {
//                DebugInfoOverlay(
//                    uiState = uiState,
//                    modifier = Modifier.align(Alignment.TopCenter)
//                )
//            }

            // Overlays
            TourProgressOverlay(
                progress = uiState.progress,
                currentStop = uiState.visitedStopsCount,
                totalStops = uiState.tourStops.size,
                modifier = Modifier.align(Alignment.TopStart)
            )

            TourStatusIndicator(
                status = uiState.tourStatus,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Debug: Show a test content card to verify rendering works
//            if (showDebugInfo && uiState.tourStops.isNotEmpty()) {
//                val testStop = uiState.tourStops.first()
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.CenterEnd)
//                        .padding(16.dp)
//                ) {
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = Color.Red.copy(alpha = 0.8f),
//                            contentColor = Color.White
//                        )
//                    ) {
//                        Column(modifier = Modifier.padding(12.dp)) {
//                            Text(
//                                "DEBUG TEST CARD",
//                                style = MaterialTheme.typography.labelSmall,
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                "If you see this, UI rendering works",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                            Text(
//                                "First stop: ${testStop.name}",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                            Text(
//                                "Has content: ${testStop.content != null}",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }
//                }
//            }

            // Debug: Manual geofence trigger for testing
            if (showDebugInfo && uiState.tourStops.isNotEmpty() && uiState.tourStatus == TourStatus.ACTIVE) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.tourStops.take(2).forEach { stop ->
                        Button(
                            onClick = {
                                Timber.d("🔧 DEBUG: Manually triggering geofence entry for ${stop.name}")
                                onIntent(LiveTourIntent.OnGeofenceEntered(stop.id))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stop.isVisited) Color.Green else Color.Blue
                            ),
                            modifier = Modifier.size(width = 120.dp, height = 32.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                "Enter ${stop.name.take(8)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            uiState.currentStop?.let { stop ->
                Timber.d("🎯 UI: Current stop check - name: ${stop.name}, isActive: ${stop.isActive}, hasContent: ${stop.content != null}")
                if (stop.isActive && stop.content != null) {
                    Timber.d("🎯 UI: SHOWING content card for stop: ${stop.name}")
                    Timber.d("🎯 UI: Content text: ${stop.content.text.take(100)}...")
                    StopContentCard(
                        content = stop.content,
                        stopName = stop.name,
                        stopOrder = stop.order,
                        audioPlayerState = uiState.audioPlayerState,
                        currentlyPlayingAudio = uiState.currentlyPlayingAudio,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        onDismiss = {
                            Timber.d("🎯 UI: Content dismissed by user")
                            onIntent(LiveTourIntent.DismissContent)
                        },
                        onPlayAudio = { audioUrl -> onIntent(LiveTourIntent.PlayAudio(audioUrl)) },
                        onPauseAudio = { onIntent(LiveTourIntent.PauseAudio) },
                        onResumeAudio = { onIntent(LiveTourIntent.ResumeAudio) },
                        onSeekAudio = { position -> onIntent(LiveTourIntent.SeekAudio(position)) }
                    )
                } else {
                    Timber.d("🎯 UI: Content card NOT shown - isActive: ${stop.isActive}, hasContent: ${stop.content != null}")
                    if (stop.content != null) {
                        Timber.d(
                            "🎯 UI: Content exists but stop not active: ${
                                stop.content.text.take(
                                    50
                                )
                            }..."
                        )
                    }
                }
            } ?: run {
                Timber.d("🎯 UI: No current stop set")
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
                16f
            )
        }

        LaunchedEffect(uiState.userLocation) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                    16f
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
            // User location marker - PROMINENT and VISIBLE
            Marker(
                state = rememberMarkerState(
                    key = "user_location_${uiState.userLocation.timestamp}",
                    position = LatLng(
                        uiState.userLocation.latitude,
                        uiState.userLocation.longitude
                    )
                ),
                title = "Your Location",
                snippet = "Accuracy: ${uiState.userLocation.accuracy.toInt()}m",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                zIndex = 1000f // Ensure it's on top
            )

            // User location accuracy circle
            Circle(
                center = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                radius = uiState.userLocation.accuracy.toDouble(),
                fillColor = Color.Blue.copy(alpha = 0.1f),
                strokeColor = Color.Blue.copy(alpha = 0.3f),
                strokeWidth = 2f,
                zIndex = 100f
            )

            // Tour stops and geofences
            uiState.tourStops.forEach { stop ->
                val stopColor = when {
                    stop.isActive -> Color(0xFF4CAF50) // Green for active
                    stop.isVisited -> Color(0xFF2196F3) // Blue for visited
                    else -> Color(0xFF9E9E9E) // Gray for unvisited
                }

                // Geofence circle
                Circle(
                    center = LatLng(stop.latitude, stop.longitude),
                    radius = stop.geofenceRadius.toDouble(),
                    fillColor = stopColor.copy(alpha = 0.15f),
                    strokeColor = stopColor.copy(alpha = 0.6f),
                    strokeWidth = 3f,
                    zIndex = 50f
                )

                // Stop marker
                Marker(
                    state = MarkerState(
                        position = LatLng(stop.latitude, stop.longitude)
                    ),
                    title = "${stop.order}. ${stop.name}",
                    snippet = when {
                        stop.isActive -> "Currently visiting"
                        stop.isVisited -> "Completed"
                        else -> "Upcoming stop"
                    },
                    icon = when {
                        stop.isActive -> BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )

                        stop.isVisited -> BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE
                        )

                        else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    },
                    zIndex = 200f
                )
            }

            // Route line connecting stops
            if (uiState.tourStops.size > 1) {
                Polyline(
                    points = uiState.tourStops.sortedBy { it.order }.map {
                        LatLng(it.latitude, it.longitude)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    width = 4f,
                    zIndex = 10f
                )
            }

            // Line from user to next stop
            uiState.nextStop?.let { nextStop ->
                Polyline(
                    points = listOf(
                        LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude),
                        LatLng(nextStop.latitude, nextStop.longitude)
                    ),
                    color = Color.Blue.copy(alpha = 0.7f),
                    width = 3f,
                    pattern = listOf(Dash(20f), Gap(10f)),
                    zIndex = 15f
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Waiting for location...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    "Make sure location services are enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    if (uiState.tourStatus == TourStatus.ACTIVE) {
        if (uiState.canComplete) {
            FloatingActionButton(
                onClick = { onIntent(LiveTourIntent.CompleteTour) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Complete tour")
            }
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
                progress = { progress.coerceIn(0f, 100f) },
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
            TourStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
            TourStatus.ACTIVE -> MaterialTheme.colorScheme.primary
            TourStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
            TourStatus.READY_TO_START -> MaterialTheme.colorScheme.background
            TourStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Text(
            text = when (status) {
                TourStatus.UPCOMING -> "PREPARING"
                TourStatus.ACTIVE -> "LIVE"
                TourStatus.COMPLETED -> "COMPLETED"
                TourStatus.READY_TO_START -> "READY TO START"
                TourStatus.CANCELLED -> "CANCELLED"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                TourStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                TourStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                TourStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiary
                TourStatus.READY_TO_START -> MaterialTheme.colorScheme.onBackground
                TourStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
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
    // Auto-dismiss timer state
    var remainingTime by remember { mutableStateOf(5) }
    var isVisible by remember { mutableStateOf(true) }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (remainingTime > 0 && isVisible) {
            delay(1000L)
            remainingTime--
        }
        if (remainingTime <= 0) {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Animated stop number indicator
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = stopOrder.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stopName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tour Stop Information",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Timer indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (5 - remainingTime) / 5f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Text(
                                text = remainingTime.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = {
                                isVisible = false
                                onDismiss()
                            },
                            modifier = Modifier.padding(0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Close",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Content section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image if available
                    if (content.imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = content.imageUrls.first(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Text content
                    if (content.text.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = content.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

                // Action hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Content will auto-close in ${remainingTime}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

//@Composable
//fun DebugInfoOverlay(
//    uiState: LiveTourUiState,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color.Black.copy(alpha = 0.8f),
//            contentColor = Color.White
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp),
//            verticalArrangement = Arrangement.spacedBy(4.dp)
//        ) {
//            Text(
//                "DEBUG INFO",
//                style = MaterialTheme.typography.labelSmall,
//                fontWeight = FontWeight.Bold,
//                color = Color.Yellow
//            )
//
//            Text("Tour ID: ${uiState.tourTitle}", style = MaterialTheme.typography.bodySmall)
//            Text("Tour Status: ${uiState.tourStatus}", style = MaterialTheme.typography.bodySmall)
//            Text(
//                "Location Enabled: ${uiState.isLocationEnabled}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Stops Count: ${uiState.tourStops.size}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Visited Count: ${uiState.visitedStopsCount}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Current Stop: ${uiState.currentStop?.name ?: "None"}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Is Active: ${uiState.currentStop?.isActive ?: false}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Has Content: ${uiState.currentStop?.content != null}",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Content Text: ${uiState.currentStop?.content?.text?.take(50) ?: "None"}...",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text(
//                "Progress: ${(uiState.progress * 100).toInt()}%",
//                style = MaterialTheme.typography.bodySmall
//            )
//            Text("Can Complete: ${uiState.canComplete}", style = MaterialTheme.typography.bodySmall)
//
//            if (uiState.userLocation != null) {
//                Text(
//                    "Location: ${uiState.userLocation.latitude.format(4)}, ${
//                        uiState.userLocation.longitude.format(
//                            4
//                        )
//                    }",
//                    style = MaterialTheme.typography.bodySmall
//                )
//            } else {
//                Text(
//                    "Location: Not available",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.Red
//                )
//            }
//
//            // Show distance to stops
//            if (uiState.userLocation != null && uiState.tourStops.isNotEmpty()) {
//                uiState.tourStops.take(3).forEach { stop ->
//                    val distance = calculateDistance(
//                        uiState.userLocation.latitude, uiState.userLocation.longitude,
//                        stop.latitude, stop.longitude
//                    )
//                    Text(
//                        "Distance to ${stop.name}: ${distance.toInt()}m (radius: ${stop.geofenceRadius}m)",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (distance <= stop.geofenceRadius) Color.Green else Color.White
//                    )
//                }
//            }
//        }
//    }
//}

// Helper function for distance calculation
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}

// Extension function for formatting coordinates
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
