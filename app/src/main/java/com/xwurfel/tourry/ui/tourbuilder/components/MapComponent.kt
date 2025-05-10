package com.xwurfel.tourry.ui.tourbuilder.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.RouteInfo
import com.xwurfel.tourry.ui.theme.primaryContainerLight
import com.xwurfel.tourry.ui.theme.primaryLight

@Composable
fun MapComponent(
    waypoints: List<WaypointDraft>,
    routeInfo: RouteInfo?,
    selectedWaypointId: String?,
    onMapClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit,
    onMapLoaded: () -> Unit,
    modifier: Modifier = Modifier,
    initialLocation: LatLng? = null
) {
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

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL
            )
        )
    }

    // Update camera when waypoints change and the camera hasn't been moved by the user
    LaunchedEffect(waypoints) {
        if (waypoints.isNotEmpty() && cameraPositionState.position.target == initialCameraPosition.target) {
            val latSum = waypoints.sumOf { it.position.latitude }
            val lngSum = waypoints.sumOf { it.position.longitude }
            val center = LatLng(latSum / waypoints.size, lngSum / waypoints.size)

            // Calculate appropriate zoom level based on distance between waypoints
            val zoom = if (waypoints.size > 1) {
                // Simple zoom calculation - can be improved for better fitBounds functionality
                13f
            } else {
                15f
            }

            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(center, zoom)
                )
            )
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

            // Draw markers for each waypoint
            waypoints.forEach { waypoint ->
                val position = waypoint.position

                Marker(
                    state = MarkerState(position = position),
                    title = waypoint.title,
                    snippet = "Stop ${waypoint.order + 1}",
                    onClick = {
                        onMarkerClick(waypoint.id ?: "")
                        true
                    },
                    icon = if (waypoint.id == selectedWaypointId) {
                        // TODO: Create custom selected marker icon
                        null
                    } else {
                        null
                    }
                )

                // Draw a circle for the geofence radius
                Circle(
                    center = position,
                    radius = waypoint.geofenceRadius.toDouble(),
                    fillColor = primaryContainerLight.copy(alpha = 0.2f),
                    strokeColor = primaryLight.copy(alpha = 0.5f),
                    strokeWidth = 2f
                )
            }
        }
    }
}