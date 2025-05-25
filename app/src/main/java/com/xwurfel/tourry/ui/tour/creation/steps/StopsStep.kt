package com.xwurfel.tourry.ui.tour.creation.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.ui.tour.creation.TourStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsStep(
    stops: List<TourStop>,
    onAddStop: (TourStop) -> Unit,
    onUpdateStop: (Int, TourStop) -> Unit,
    onRemoveStop: (Int) -> Unit,
    onReorderStops: (Int, Int) -> Unit
) {
    var showMapDialog by remember { mutableStateOf(false) }
    var editingStopIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map preview with stops
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(48.8566, 2.3522), 12f
                    )
                }
            ) {
                stops.forEach { stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = stop.name
                    )
                }
            }

            // Add stop FAB
            FloatingActionButton(
                onClick = { showMapDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add stop")
            }
        }

        // Stops list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(stops, key = { _, item -> item.id }) { index, stop ->
                StopCard(
                    stop = stop,
                    index = index,
                    isFirst = index == 0,
                    isLast = index == stops.lastIndex,
                    onEdit = { editingStopIndex = index },
                    onRemove = { onRemoveStop(index) },
                    onMoveUp = { onReorderStops(index, index - 1) },
                    onMoveDown = { onReorderStops(index, index + 1) }
                )
            }
        }
    }

    // Add/Edit stop dialog
    if (showMapDialog || editingStopIndex != null) {
        StopEditDialog(
            stop = editingStopIndex?.let { stops[it] },
            onDismiss = {
                showMapDialog = false
                editingStopIndex = null
            },
            onSave = { stop ->
                if (editingStopIndex != null) {
                    onUpdateStop(editingStopIndex!!, stop)
                } else {
                    onAddStop(stop)
                }
                showMapDialog = false
                editingStopIndex = null
            }
        )
    }
}

@Composable
fun StopCard(
    stop: TourStop,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${index + 1}. ${stop.name}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stop.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reorder buttons
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (isFirst)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (isLast)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopEditDialog(
    stop: TourStop?,
    onDismiss: () -> Unit,
    onSave: (TourStop) -> Unit
) {
    var name by remember(stop) { mutableStateOf(stop?.name ?: "") }
    var description by remember(stop) { mutableStateOf(stop?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (stop == null) "Add Stop" else "Edit Stop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stop name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3
                )

                // TODO: Add map picker for location
                // TODO: Add media upload
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        stop?.copy(name = name, description = description)
                            ?: TourStop(
                                name = name,
                                description = description,
                                latitude = 48.8566, // TODO: Get from map
                                longitude = 2.3522
                            )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}