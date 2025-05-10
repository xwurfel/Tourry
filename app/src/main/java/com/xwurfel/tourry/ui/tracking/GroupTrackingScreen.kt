package com.xwurfel.tourry.ui.tracking

import android.Manifest
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.feature.service.location.LocationTrackingService
import com.xwurfel.tourry.feature.tracking.domain.model.GroupMember
import com.xwurfel.tourry.feature.tracking.domain.model.GroupStatus
import com.xwurfel.tourry.feature.tracking.domain.model.MemberLocation
import com.xwurfel.tourry.feature.tracking.domain.model.MemberStatus
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GroupTrackingScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    viewModel: GroupTrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Request location permission if not granted
    LaunchedEffect(locationPermissionState) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Load group data
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    // Show error messages in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Start location updates when permission is granted
    LaunchedEffect(locationPermissionState.status.isGranted, state.group) {
        if (locationPermissionState.status.isGranted && state.group != null) {
            // Get the current location
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.updateCurrentLocation(it)
                }
            }
        }
    }

    // Start/stop location tracking service based on tracking state
    LaunchedEffect(state.isTracking, state.group) {
        val group = state.group ?: return@LaunchedEffect
        val currentUserId = state.currentUserId ?: return@LaunchedEffect

        if (state.isTracking) {
            LocationTrackingService.startTracking(
                context = context,
                groupId = group.id,
                userId = currentUserId,
                groupName = group.name
            )
        } else {
            LocationTrackingService.stopTracking(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (state.isTracking) {
                    LocationTrackingService.stopTracking(context)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // Get the markers for the map
    val memberMarkers = state.membersLocations.map { (userId, location) ->
        val member = state.group?.members?.find { it.userId == userId } ?: return@map null
        val isGuide = userId == state.group?.guideId
        val isCurrentUser = userId == state.currentUserId

        MemberMarker(
            member = member,
            location = location,
            isGuide = isGuide,
            isCurrentUser = isCurrentUser
        )
    }.filterNotNull()

    val guideLocation = state.guideLocation
    val geofenceRadius = state.geofenceSettings.radiusMeters

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            GroupMembersSheet(
                members = state.group?.members ?: emptyList(),
                membersLocations = state.membersLocations,
                currentUserId = state.currentUserId,
                guideId = state.group?.guideId
            )
        },
        sheetPeekHeight = 128.dp,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.group == null) {
                Text(
                    text = "Failed to load group",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                GroupTrackingContent(
                    isLocationPermissionGranted = locationPermissionState.status.isGranted,
                    memberMarkers = memberMarkers,
                    guideLocation = guideLocation,
                    geofenceRadius = geofenceRadius,
                    isTracking = state.isTracking,
                    isUserGuide = state.isUserGuide,
                    isUserInGeofence = state.isUserInGeofence,
                    groupStatus = state.group?.status ?: GroupStatus.CREATED,
                    onStartTracking = { viewModel.startTracking() },
                    onStopTracking = { viewModel.stopTracking() },
                    onRegroupRequest = { viewModel.requestRegroup() },
                    onStartTour = { viewModel.startTour() },
                    onPauseTour = { viewModel.pauseTour() },
                    onEndTour = { viewModel.endTour() },
                    onUpdateGeofenceRadius = {
                        viewModel.updateGeofenceSettings(
                            state.geofenceSettings.copy(radiusMeters = it)
                        )
                    },
                    onBackPressed = onNavigateBack
                )

                // Warning banner if user is out of geofence
                if (!state.isUserInGeofence) {
                    OutOfGeofenceWarning(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GroupTrackingContent(
    isLocationPermissionGranted: Boolean,
    memberMarkers: List<MemberMarker>,
    guideLocation: MemberLocation?,
    geofenceRadius: Float,
    isTracking: Boolean,
    isUserGuide: Boolean,
    isUserInGeofence: Boolean,
    groupStatus: GroupStatus,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onRegroupRequest: () -> Unit,
    onStartTour: () -> Unit,
    onPauseTour: () -> Unit,
    onEndTour: () -> Unit,
    onUpdateGeofenceRadius: (Float) -> Unit,
    onBackPressed: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showGeofenceSettings by remember { mutableStateOf(false) }
    var geofenceRadiusValue by remember { mutableFloatStateOf(geofenceRadius) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = isLocationPermissionGranted
            ),
            onMapClick = { /* Handle map click if needed */ }
        ) {
            // Add markers for all members
            memberMarkers.forEach { memberMarker ->
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            memberMarker.location.latitude,
                            memberMarker.location.longitude
                        )
                    ),
                    title = memberMarker.member.name,
                    snippet = if (memberMarker.isGuide) "Guide" else "Participant",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when {
                            memberMarker.isGuide -> BitmapDescriptorFactory.HUE_RED
                            memberMarker.isCurrentUser -> BitmapDescriptorFactory.HUE_AZURE
                            else -> BitmapDescriptorFactory.HUE_GREEN
                        }
                    )
                )
            }

            // Draw geofence circle around guide
            guideLocation?.let {
                Circle(
                    center = LatLng(it.latitude, it.longitude),
                    radius = geofenceRadius.toDouble(),
                    fillColor = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.1f),
                    strokeColor = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.5f),
                    strokeWidth = 2f
                )
            }

            // Adjust camera to show all markers
            MapEffect(memberMarkers) { map ->
                if (memberMarkers.isNotEmpty()) {
                    val boundsBuilder = LatLngBounds.builder()
                    memberMarkers.forEach { marker ->
                        boundsBuilder.include(
                            LatLng(marker.location.latitude, marker.location.longitude)
                        )
                    }

                    coroutineScope.launch {
                        try {
                            val bounds = boundsBuilder.build()
                            map.animateCamera(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    100
                                )
                            )
                        } catch (_: Exception) {
                            memberMarkers.firstOrNull()?.let { marker ->
                                val position = LatLng(
                                    marker.location.latitude,
                                    marker.location.longitude
                                )
                                map.animateCamera(
                                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                        position,
                                        15f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            if (isUserGuide) {
                IconButton(
                    onClick = { showGeofenceSettings = !showGeofenceSettings },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Geofence Settings"
                    )
                }
            }
        }

        // Geofence Settings Sheet
        if (showGeofenceSettings) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Geofence Radius",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "25m")

                        Slider(
                            value = geofenceRadiusValue,
                            onValueChange = { geofenceRadiusValue = it },
                            valueRange = 25f..200f,
                            steps = 7, // 25m steps
                            modifier = Modifier.weight(1f)
                        )

                        Text(text = "200m")
                    }

                    Text(
                        text = "${geofenceRadiusValue.toInt()}m",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onUpdateGeofenceRadius(geofenceRadiusValue)
                                showGeofenceSettings = false
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        // Action buttons at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Tracking control buttons
            if (isUserGuide) {
                // Guide controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    when (groupStatus) {
                        GroupStatus.CREATED -> {
                            Button(
                                onClick = onStartTour
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Start Tour")
                            }
                        }

                        GroupStatus.ACTIVE -> {
                            OutlinedButton(
                                onClick = onPauseTour
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Pause")
                            }

                            Button(
                                onClick = onEndTour
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("End Tour")
                            }
                        }

                        GroupStatus.PAUSED -> {
                            Button(
                                onClick = onStartTour
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Resume")
                            }

                            OutlinedButton(
                                onClick = onEndTour
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("End Tour")
                            }
                        }

                        else -> {
                            // No buttons for completed or cancelled tours
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Regroup button (always available for guide)
                if (groupStatus == GroupStatus.ACTIVE || groupStatus == GroupStatus.PAUSED) {
                    Button(
                        onClick = onRegroupRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.PeopleAlt, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Request Regroup")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // Participant controls
                if (groupStatus == GroupStatus.ACTIVE || groupStatus == GroupStatus.PAUSED) {
                    Button(
                        onClick = if (isTracking) onStopTracking else onStartTracking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isTracking) "Stop Tracking" else "Start Tracking")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun GroupMembersSheet(
    members: List<GroupMember>,
    membersLocations: Map<String, MemberLocation>,
    currentUserId: String?,
    guideId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Group Members",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(members) { member ->
                MemberItem(
                    member = member,
                    location = membersLocations[member.userId],
                    isCurrentUser = member.userId == currentUserId,
                    isGuide = member.userId == guideId
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MemberItem(
    member: GroupMember,
    location: MemberLocation?,
    isCurrentUser: Boolean,
    isGuide: Boolean
) {
    val lastLocationText = location?.let {
        val ageMinutes = ChronoUnit.MINUTES.between(it.timestamp, Instant.now())
        when {
            ageMinutes < 1 -> "Just now"
            ageMinutes == 1L -> "1 minute ago"
            ageMinutes < 60 -> "$ageMinutes minutes ago"
            else -> "Over 1 hour ago"
        }
    } ?: "No location data"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar or icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isGuide) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isGuide) {
                        Text(
                            text = " (Guide)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isCurrentUser) {
                        Text(
                            text = " (You)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Text(
                    text = "Last seen: $lastLocationText",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (member.status) {
                            MemberStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            MemberStatus.JOINED -> MaterialTheme.colorScheme.secondary
                            MemberStatus.INACTIVE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun OutOfGeofenceWarning(modifier: Modifier = Modifier) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if ((System.currentTimeMillis() / 500) % 2 == 0L) 1f else 0.7f,
        label = "Warning Animation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = animatedAlpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "You're outside the tour area!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Data class to hold marker information
data class MemberMarker(
    val member: GroupMember,
    val location: MemberLocation,
    val isGuide: Boolean,
    val isCurrentUser: Boolean
)