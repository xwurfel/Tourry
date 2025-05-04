package com.xwurfel.tourry.presentation.tour.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent

@Composable
fun RouteEditorScreenRoute(
    tourId: Long, onNavigateBack: () -> Unit
) {
    val viewModel: RouteEditorViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tourId) {
        viewModel.loadTourData(tourId)
    }

    when {
        uiState.isLoading -> {
            LoadingScreenContent(Modifier.fillMaxSize())
        }

        uiState.errorMessage != null -> {
            ErrorScreenContent(
                errorMessage = uiState.errorMessage ?: "An error occurred",
                modifier = Modifier.fillMaxSize()
            )
        }

        else -> {
            RouteEditorScreen(
                uiState = uiState,
                onAddRoutePoint = viewModel::addRoutePoint,
                onRemoveRoutePoint = viewModel::removeRoutePoint,
                onUpdateRoutePoint = viewModel::updateRoutePoint,
                onSaveRoute = viewModel::saveRoute,
                onNavigateBack = {
                    if (uiState.hasUnsavedChanges) {
                        viewModel.showDiscardChangesDialog()
                    } else {
                        onNavigateBack()
                    }
                },
                onConfirmDiscard = {
                    viewModel.hideDiscardChangesDialog()
                    onNavigateBack()
                },
                onCancelDiscard = {
                    viewModel.hideDiscardChangesDialog()
                })
        }
    }

    if (uiState.isSaved) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(
    uiState: RouteEditorUiState,
    onAddRoutePoint: (LatLng) -> Unit,
    onRemoveRoutePoint: (Int) -> Unit,
    onUpdateRoutePoint: (Int, RoutePoint) -> Unit,
    onSaveRoute: () -> Unit,
    onNavigateBack: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    var isMapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(isMapLoaded, uiState.routePoints) {
        if (isMapLoaded && uiState.routePoints.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            uiState.routePoints.forEach { point ->
                builder.include(point.location)
            }

            try {
                val bounds = builder.build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            } catch (_: IllegalStateException) {
                if (uiState.routePoints.isNotEmpty()) {
                    val firstPoint = uiState.routePoints.first().location
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(firstPoint, 15f)
                        )
                    )
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Edit Tour Route") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                )
            }
        }, actions = {
            IconButton(onClick = onSaveRoute, enabled = uiState.routePoints.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Check, contentDescription = "Save Route"
                )
            }
        })
    }, bottomBar = {
        BottomAppBar(actions = {
            Text(
                text = "Tap on the map to add route points",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Toggle editing mode
                }) {
                Icon(
                    imageVector = Icons.Default.Edit, contentDescription = "Edit Mode"
                )
            }
        })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.NORMAL, isMyLocationEnabled = true
                ),
                onMapLoaded = {
                    isMapLoaded = true
                },
                onMapClick = { latLng ->
                    onAddRoutePoint(latLng)
                }) {
                uiState.routePoints.forEachIndexed { index, point ->
                    Marker(
                        state = MarkerState(position = point.location),
                        title = "Stop ${index + 1}: ${point.title}",
                        snippet = point.description,
                        draggable = true,
                        // TODO: FIX THIS - No parameter with name 'onDragEnd' found.
//                        onDragEnd = { latLng ->
//                            onUpdateRoutePoint(
//                                index, point.copy(location = latLng)
//                            )
//                        }
                    )
                }

                if (uiState.routePoints.size > 1) {
                    Polyline(
                        points = uiState.routePoints.map { it.location },
                        color = Color.Blue,
                        width = 5f
                    )
                }
            }

            if (uiState.selectedRoutePointIndex != -1) {
                val selectedPoint = uiState.routePoints.getOrNull(uiState.selectedRoutePointIndex)
                selectedPoint?.let { point ->
                    RoutePointEditorCard(
                        routePoint = point,
                        index = uiState.selectedRoutePointIndex,
                        onUpdateRoutePoint = onUpdateRoutePoint,
                        onRemoveRoutePoint = onRemoveRoutePoint,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // Discard changes confirmation dialog
    if (uiState.showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) {
                    Text("Cancel")
                }
            })
    }
}

@Composable
fun RoutePointEditorCard(
    routePoint: RoutePoint,
    index: Int,
    onUpdateRoutePoint: (Int, RoutePoint) -> Unit,
    onRemoveRoutePoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(routePoint.title) }
    var description by remember { mutableStateOf(routePoint.description) }
    var durationMinutes by remember { mutableStateOf((routePoint.durationMinutes ?: 0).toString()) }

    Card(
        modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Stop ${index + 1}", style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title, onValueChange = {
                    title = it
                    onUpdateRoutePoint(index, routePoint.copy(title = it))
                }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description, onValueChange = {
                    description = it
                    onUpdateRoutePoint(index, routePoint.copy(description = it))
                }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = durationMinutes, onValueChange = {
                    durationMinutes = it
                    val minutes = it.toIntOrNull() ?: 0
                    onUpdateRoutePoint(index, routePoint.copy(durationMinutes = minutes))
                }, label = { Text("Duration (minutes)") }, modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onRemoveRoutePoint(index) }) {
                    Icon(
                        imageVector = Icons.Default.Delete, contentDescription = "Delete"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}