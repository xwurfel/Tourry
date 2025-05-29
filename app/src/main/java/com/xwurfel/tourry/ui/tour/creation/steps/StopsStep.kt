package com.xwurfel.tourry.ui.tour.creation.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.ui.tour.creation.TourStop
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsStep(
    stops: List<TourStop>,
    onAddStop: (TourStop) -> Unit,
    onUpdateStop: (Int, TourStop) -> Unit,
    onRemoveStop: (Int) -> Unit,
    onReorderStops: (Int, Int) -> Unit
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var editingStopIndex by remember { mutableStateOf<Int?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map preview with stops
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    if (stops.isNotEmpty())
                        LatLng(stops.first().latitude, stops.first().longitude)
                    else
                        LatLng(48.8566, 2.3522),
                    12f
                )
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    showStopDialog = true
                }
            ) {
                stops.forEachIndexed { index, stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "${index + 1}. ${stop.name}"
                    )
                }
            }

            // Instruction overlay
            if (stops.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "Tap on the map to add your first stop",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Stops list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(stops, key = { _, item -> item.id }) { index, stop ->
                EnhancedStopCard(
                    stop = stop,
                    index = index,
                    isFirst = index == 0,
                    isLast = index == stops.lastIndex,
                    onEdit = {
                        editingStopIndex = index
                        selectedLocation = LatLng(stop.latitude, stop.longitude)
                        showStopDialog = true
                    },
                    onRemove = { onRemoveStop(index) },
                    onMoveUp = { onReorderStops(index, index - 1) },
                    onMoveDown = { onReorderStops(index, index + 1) }
                )
            }
        }
    }

    // Enhanced Add/Edit stop dialog
    if (showStopDialog) {
        EnhancedStopEditDialog(
            stop = editingStopIndex?.let { stops[it] },
            selectedLocation = selectedLocation,
            onDismiss = {
                showStopDialog = false
                editingStopIndex = null
                selectedLocation = null
            },
            onSave = { stop ->
                if (editingStopIndex != null) {
                    onUpdateStop(editingStopIndex!!, stop)
                } else {
                    onAddStop(stop)
                }
                showStopDialog = false
                editingStopIndex = null
                selectedLocation = null
            }
        )
    }
}

@Composable
fun EnhancedStopCard(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${index + 1}. ${stop.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stop.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Control buttons
                Row {
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

            // Media preview
            if (stop.mediaUrls.isNotEmpty() || stop.audioUrl != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image previews
                    if (stop.mediaUrls.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${stop.mediaUrls.size} image${if (stop.mediaUrls.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Audio indicator
                    if (stop.audioUrl != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Audio guide",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStopEditDialog(
    stop: TourStop?,
    selectedLocation: LatLng?,
    onDismiss: () -> Unit,
    onSave: (TourStop) -> Unit
) {
    var name by remember(stop) { mutableStateOf(stop?.name ?: "") }
    var description by remember(stop) { mutableStateOf(stop?.description ?: "") }
    var mediaUrls by remember(stop) { mutableStateOf(stop?.mediaUrls ?: emptyList()) }
    var audioUrl by remember(stop) { mutableStateOf(stop?.audioUrl) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newUrls = uris.map { uri ->
            // In real app, upload to server and get URL
            // For now, convert to string for mock
            uri.toString()
        }
        mediaUrls = mediaUrls + newUrls
    }

    // Audio picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // In real app, upload to server and get URL
            audioUrl = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (stop == null) "Add Stop" else "Edit Stop") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Stop name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Location display
                item {
                    selectedLocation?.let { location ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Location: ${
                                        String.format(
                                            Locale.getDefault(),
                                            "%.4f",
                                            location.latitude
                                        )
                                    }, ${
                                        String.format(
                                            Locale.getDefault(),
                                            "%.4f",
                                            location.longitude
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Media upload section
                item {
                    Text(
                        "Media",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Images
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Images",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Images")
                            }
                        }

                        if (mediaUrls.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(mediaUrls) { imageUrl ->
                                    Box {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                mediaUrls = mediaUrls.filter { it != imageUrl }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Audio Guide",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch("audio/*") }
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (audioUrl != null) "Replace" else "Add Audio")
                            }
                        }

                        audioUrl?.let {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AudioFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "Audio guide attached",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { audioUrl = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val location =
                        selectedLocation ?: stop?.let { LatLng(it.latitude, it.longitude) }
                    if (location != null && name.isNotBlank()) {
                        val newStop = stop?.copy(
                            name = name,
                            description = description,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            mediaUrls = mediaUrls,
                            audioUrl = audioUrl
                        ) ?: TourStop(
                            name = name,
                            description = description,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            mediaUrls = mediaUrls,
                            audioUrl = audioUrl
                        )
                        onSave(newStop)
                    }
                },
                enabled = name.isNotBlank() && (selectedLocation != null || stop != null)
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