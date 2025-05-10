package com.xwurfel.tourry.ui.tourbuilder.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.R
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.RouteInfo
import com.xwurfel.tourry.ui.theme.primaryContainerLight
import com.xwurfel.tourry.ui.theme.primaryLight
import com.xwurfel.tourry.ui.util.BitmapDescriptorUtil
import kotlin.math.max
import kotlin.math.min

@Composable
fun MapComponent(
    waypoints: List<WaypointDraft>,
    routeInfo: RouteInfo?,
    selectedWaypointId: String?,
    onMapClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit,
    onMapLoaded: () -> Unit,
    modifier: Modifier = Modifier,
    initialLocation: LatLng? = null,
    hasLocationPermission: Boolean = false,
    onMyLocationClick: () -> Unit
) {
    val context = LocalContext.current
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }
    val mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL
            )
        )
    }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentUserLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            } catch (_: SecurityException) {
                // Permission denied or removed
            }
        }

        onDispose {}
    }

    val pulseAnimationValue by animateFloatAsState(
        targetValue = if ((System.currentTimeMillis() / 1000) % 2 == 0L) 1f else 0.6f,
        label = "Pulse Animation"
    )

    val defaultMarkerIcon = remember(context) {
        BitmapDescriptorUtil.vectorToBitmap(context, R.drawable.ic_marker_default)
    }

    val selectedMarkerIcon = remember(context) {
        BitmapDescriptorUtil.vectorToBitmap(context, R.drawable.ic_marker_selected, primaryLight)
    }

    val initialCameraPosition = if (waypoints.isNotEmpty()) {
        val latSum = waypoints.sumOf { it.position.latitude }
        val lngSum = waypoints.sumOf { it.position.longitude }
        val center = LatLng(latSum / waypoints.size, lngSum / waypoints.size)
        CameraPosition.fromLatLngZoom(center, 13f)
    } else if (initialLocation != null) {
        CameraPosition.fromLatLngZoom(initialLocation, 13f)
    } else {
        // Default to Lviv
        CameraPosition.fromLatLngZoom(LatLng(49.8419, 24.0315), 13f)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = initialCameraPosition
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        )
    }

    LaunchedEffect(waypoints) {
        if (waypoints.isNotEmpty()) {
            if (waypoints.size == 1) {
                val position = waypoints[0].position
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(position, 15f)
                    )
                )
            } else {
                calculateOptimalCameraPosition(waypoints.map { it.position })?.let { cameraUpdate ->
                    cameraPositionState.animate(cameraUpdate)
                }
            }
        }
    }

    // Update camera when a waypoint is selected
    LaunchedEffect(selectedWaypointId) {
        if (selectedWaypointId != null) {
            val selectedWaypoint =
                waypoints.firstOrNull { it.id == selectedWaypointId } ?: return@LaunchedEffect
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(
                    selectedWaypoint.position
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                onMapClick(latLng)
            },
            onMapLoaded = onMapLoaded
        ) {
            // Draw route path if available
            if (routeInfo != null && routeInfo.path.isNotEmpty()) {
                val pathPoints = routeInfo.path.map { it }

                Polyline(
                    points = pathPoints,
                    color = primaryLight,
                    width = 5f
                )
            }

            waypoints.forEach { waypoint ->
                val position = waypoint.position
                val isSelected = waypoint.id == selectedWaypointId

                Marker(
                    state = MarkerState(position = position),
                    title = waypoint.title,
                    snippet = "Stop ${waypoint.order + 1}",
                    onClick = {
                        onMarkerClick(waypoint.id ?: "")
                        true
                    },
                    icon = if (isSelected) selectedMarkerIcon else defaultMarkerIcon,
                    zIndex = if (isSelected) 1f else 0f
                )

                Circle(
                    center = position,
                    radius = waypoint.geofenceRadius.toDouble(),
                    fillColor = primaryContainerLight.copy(alpha = 0.2f),
                    strokeColor = primaryLight.copy(alpha = 0.5f),
                    strokeWidth = 2f
                )
            }

            currentUserLocation?.let { userLoc ->
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAnimationValue)
                ) {}

                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}

private fun calculateOptimalCameraPosition(positions: List<LatLng>): CameraUpdate? {
    if (positions.isEmpty()) return null
    if (positions.size == 1) return CameraUpdateFactory.newLatLngZoom(positions[0], 15f)

    // Find the bounds of all positions
    var north = -90.0
    var south = 90.0
    var east = -180.0
    var west = 180.0

    positions.forEach { position ->
        north = max(north, position.latitude)
        south = min(south, position.latitude)
        east = max(east, position.longitude)
        west = min(west, position.longitude)
    }

    // Create a bounds object
    val bounds = LatLngBounds(
        LatLng(south, west),
        LatLng(north, east)
    )

    // Add padding (in pixels)
    return CameraUpdateFactory.newLatLngBounds(bounds, 100)
}