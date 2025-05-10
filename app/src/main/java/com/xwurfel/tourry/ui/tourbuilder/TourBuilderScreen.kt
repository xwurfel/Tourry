package com.xwurfel.tourry.ui.tourbuilder

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentType
import com.xwurfel.tourry.ui.tourbuilder.components.ContentEditor
import com.xwurfel.tourry.ui.tourbuilder.components.MapComponent
import com.xwurfel.tourry.ui.tourbuilder.components.TourInfoEditor
import com.xwurfel.tourry.ui.tourbuilder.components.WaypointDetailPanel
import com.xwurfel.tourry.ui.tourbuilder.components.WaypointEditor
import com.xwurfel.tourry.ui.tourbuilder.components.WaypointList
import com.xwurfel.tourry.util.file.FileUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TourBuilderScreen(
    tourId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToTourDetails: (String) -> Unit,
    viewModel: TourBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Initialize tour if ID is provided
    LaunchedEffect(tourId) {
        if (tourId != null) {
            viewModel.loadTourDraft(tourId)
        }
    }

    // Location permission handling
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var showSaveDialog by remember { mutableStateOf(false) }
    var showInfoEditor by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Media upload handling
    var mediaContentType by remember { mutableStateOf<ContentType?>(null) }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            FileUtil.createTempFileFromUri(context, uri, "image")?.let { file ->
                val compressedFile = FileUtil.compressImageIfNeeded(file)
                viewModel.uploadImage(compressedFile)
            }
        }
    }

    val selectAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            FileUtil.createTempFileFromUri(context, uri, "audio")?.let { file ->
                viewModel.uploadAudio(file)
            }
        }
    }

    LaunchedEffect(state.operationSuccess, state.tourDraft.id) {
        if (state.operationSuccess?.contains("saved") == true ||
            state.operationSuccess?.contains("published") == true
        ) {
            state.tourDraft.id?.let { id ->
                onNavigateToTourDetails(id)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.operationSuccess) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            viewModel.dismissMessage()
        }

        state.operationSuccess?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.tourDraft.id == null) "Create New Tour"
                        else "Edit Tour"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Info button
                    IconButton(onClick = { showInfoEditor = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Edit Tour Info")
                    }

                    // Save button
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Tour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Only show if map is ready and not editing
            if (state.isMapReady && !state.isEditingWaypoint && !state.isEditingContent) {
                FloatingActionButton(
                    onClick = {
                        if (state.selectedWaypointId != null) {
                            viewModel.selectWaypoint(null)
                        } else {
                            // TODO: Focus on current location on map
                        }
                    }
                ) {
                    Icon(
                        if (state.selectedWaypointId != null)
                            Icons.Default.Close
                        else
                            Icons.Default.MyLocation,
                        contentDescription = if (state.selectedWaypointId != null)
                            "Clear Selection"
                        else
                            "My Location"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main map view
            MapComponent(
                waypoints = state.tourDraft.waypoints,
                routeInfo = state.routeInfo,
                selectedWaypointId = state.selectedWaypointId,
                onMapClick = { position ->
                    if (!state.isEditingWaypoint && !state.isEditingContent) {
                        viewModel.addWaypoint(position)
                    }
                },
                onMarkerClick = { waypointId ->
                    if (!state.isEditingWaypoint && !state.isEditingContent) {
                        viewModel.selectWaypoint(waypointId)
                    }
                },
                onMapLoaded = { viewModel.setMapReady(true) },
                modifier = Modifier.fillMaxSize()
            )

            // Editors and panels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                // Show waypoint editor when editing
                if (state.isEditingWaypoint && state.selectedWaypointId != null) {
                    val waypoint =
                        state.tourDraft.waypoints.firstOrNull { it.id == state.selectedWaypointId }
                    if (waypoint != null) {
                        WaypointEditor(
                            waypoint = waypoint,
                            onSave = { title, description, durationMinutes, geofenceRadius ->
                                viewModel.updateWaypoint(
                                    waypointId = waypoint.id ?: "",
                                    title = title,
                                    description = description,
                                    durationMinutes = durationMinutes,
                                    geofenceRadius = geofenceRadius
                                )
                            },
                            onCancel = { viewModel.cancelEditing() },
                            onDelete = { viewModel.deleteWaypoint(waypoint.id ?: "") }
                        )
                    }
                } else if (state.isEditingContent && state.selectedWaypointId != null) {
                    val waypoint =
                        state.tourDraft.waypoints.firstOrNull { it.id == state.selectedWaypointId }
                    if (waypoint != null) {
                        val selectedContent = if (state.selectedContentId != null) {
                            waypoint.contents.firstOrNull { it.id == state.selectedContentId }
                        } else null

                        ContentEditor(
                            content = selectedContent,
                            onSave = { title, description, type, mediaUrl ->
                                if (selectedContent != null) {
                                    viewModel.updateContent(
                                        waypointId = state.selectedWaypointId!!,
                                        contentId = selectedContent.id ?: "",
                                        title = title,
                                        description = description,
                                        type = type,
                                        mediaUrl = mediaUrl
                                    )
                                } else {
                                    viewModel.addContent(
                                        waypointId = state.selectedWaypointId!!,
                                        title = title,
                                        description = description,
                                        type = type,
                                        mediaUrl = mediaUrl
                                    )
                                }
                            },
                            onCancel = { viewModel.cancelEditing() },
                            onRequestMediaUpload = { contentType ->
                                mediaContentType = contentType
                                if (contentType == ContentType.IMAGE) {
                                    selectImageLauncher.launch("image/*")
                                } else if (contentType == ContentType.AUDIO) {
                                    selectAudioLauncher.launch("audio/*")
                                }
                            }
                        )
                    }
                } else if (!state.isEditingWaypoint && !state.isEditingContent) {
                    if (state.selectedWaypointId != null) {
                        val waypoint =
                            state.tourDraft.waypoints.firstOrNull { it.id == state.selectedWaypointId }
                        if (waypoint != null) {
                            WaypointDetailPanel(
                                waypoint = waypoint,
                                onEdit = { viewModel.startEditingWaypoint() },
                                onAddContent = { viewModel.startEditingContent() },
                                onContentSelected = { contentId ->
                                    viewModel.selectContent(contentId)
                                },
                                onClose = { viewModel.selectWaypoint(null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        WaypointList(
                            waypoints = state.tourDraft.waypoints,
                            selectedWaypointId = state.selectedWaypointId,
                            onWaypointSelected = { waypointId -> viewModel.selectWaypoint(waypointId) },
                            onWaypointEdit = { waypointId ->
                                viewModel.selectWaypoint(waypointId)
                                viewModel.startEditingWaypoint()
                            },
                            onWaypointsReordered = { fromIndex, toIndex ->
                                viewModel.reorderWaypoints(fromIndex, toIndex)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.isLoading || state.uploadInProgress || state.saveInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Tour info editor dialog
    if (showInfoEditor) {
        AlertDialog(
            onDismissRequest = { showInfoEditor = false },
            title = { Text("Tour Information") },
            text = {
                TourInfoEditor(
                    tourDraft = state.tourDraft,
                    onSave = { title, description, location, category, difficulty, price, isPublic ->
                        viewModel.updateTourBasicInfo(
                            title = title,
                            description = description,
                            location = location,
                            category = category,
                            difficulty = difficulty,
                            price = price,
                            isPublic = isPublic
                        )
                        showInfoEditor = false
                    }
                )
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showInfoEditor = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Tour") },
            text = {
                Column {
                    Text("Do you want to save this tour as a draft or publish it publicly?")

                    if (state.tourDraft.id != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                showSaveDialog = false
                                showDeleteConfirmation = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Tour")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.publishTour()
                        showSaveDialog = false
                    }
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.saveTour()
                        showSaveDialog = false
                    }
                ) {
                    Text("Save as Draft")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Tour?") },
            text = { Text("This will permanently delete this tour and all its waypoints and content. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTour()
                        showDeleteConfirmation = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}