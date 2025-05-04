package com.xwurfel.tourry.presentation.tour.checkin

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.R
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.presentation.common.ErrorDialogContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import com.xwurfel.tourry.presentation.common.SuccessDialogContent
import kotlinx.coroutines.delay

@Composable
fun TourCheckInScreenRoute(
    tourId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: TourCheckInViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tourId) {
        viewModel.loadTourCheckInData(tourId)
    }

    // Refresh location periodically
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateUserLocation()
            delay(10000) // Update every 10 seconds
        }
    }

    when {
        uiState.isLoading -> {
            LoadingScreenContent(Modifier.fillMaxSize())
        }

        uiState.errorMessage != null -> {
            ErrorDialogContent(
                errorMessage = uiState.errorMessage ?: "An error occurred",
                onDismiss = viewModel::clearErrorMessage
            )
        }

        else -> {
            TourCheckInScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                onNavigateBack = onNavigateBack,
                onRoutePointSelected = viewModel::selectRoutePoint,
                onCheckInNoteChanged = viewModel::onCheckInNoteChanged,
                onCheckInImageSelected = viewModel::onCheckInImageSelected,
                onCheckInClicked = { viewModel.checkInToSelectedPoint() },
                onForceCheckInClicked = { viewModel.checkInToSelectedPoint(forceCheckIn = true) }
            )
        }
    }

    if (uiState.showSuccessMessage && uiState.successMessage != null) {
        SuccessDialogContent(
            message = uiState.successMessage!!,
            onDismiss = viewModel::clearSuccessMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourCheckInScreen(
    uiState: TourCheckInUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRoutePointSelected: (Long) -> Unit,
    onCheckInNoteChanged: (String) -> Unit,
    onCheckInImageSelected: (Uri) -> Unit,
    onCheckInClicked: () -> Unit,
    onForceCheckInClicked: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    var showMap by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.routePoints, showMap) {
        if (uiState.routePoints.isNotEmpty() && showMap) {
            try {
                val builder = LatLngBounds.Builder()
                uiState.routePoints.forEach { point ->
                    builder.include(point.location)
                }

                uiState.userLocation?.let { builder.include(it) }

                val bounds = builder.build()
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                    durationMs = 1000
                )
            } catch (_: Exception) {
                val firstPoint = uiState.routePoints.first().location
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(firstPoint, 13f)
                    )
                )
            }
        } else if (uiState.userLocation != null && showMap) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(uiState.userLocation, 15f)
                )
            )
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressPercentage,
        animationSpec = tween(durationMillis = 1000),
        label = "Progress Animation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.tour?.title ?: "Tour Check-In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showMap) {
                FloatingActionButton(
                    onClick = { showMap = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Show Map"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showMap) {
                MapView(
                    uiState = uiState,
                    cameraPositionState = cameraPositionState,
                    onRoutePointSelected = onRoutePointSelected,
                    onCloseMap = { showMap = false }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )

                    Text(
                        text = "${(animatedProgress * 100).toInt()}% Complete",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RoutePointsList(
                        routePoints = uiState.routePoints,
                        checkedInPoints = uiState.checkedInPoints,
                        selectedRoutePointId = uiState.selectedRoutePointId,
                        onRoutePointSelected = onRoutePointSelected,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(
                        visible = uiState.selectedRoutePointId != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        uiState.selectedRoutePointId?.let { selectedId ->
                            val selectedPoint = uiState.routePoints.find { it.id == selectedId }
                            if (selectedPoint != null && !uiState.checkedInPoints.contains(
                                    selectedId
                                )
                            ) {
                                CheckInCard(
                                    routePoint = selectedPoint,
                                    checkInNote = uiState.checkInNote,
                                    checkInImageUri = uiState.checkInImageUri,
                                    isCheckingIn = uiState.isCheckingIn,
                                    onNoteChanged = onCheckInNoteChanged,
                                    onImageSelected = onCheckInImageSelected,
                                    onCheckInClicked = onCheckInClicked,
                                    onForceCheckInClicked = onForceCheckInClicked,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapView(
    uiState: TourCheckInUiState,
    cameraPositionState: CameraPositionState,
    onRoutePointSelected: (Long) -> Unit,
    onCloseMap: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            uiState.routePoints.forEach { point ->
                val isCheckedIn = uiState.checkedInPoints.contains(point.id)
                Marker(
                    state = MarkerState(position = point.location),
                    title = point.title,
                    snippet = point.description,
                    icon = if (isCheckedIn) {
                        Color.Green.toBitmapDescriptor(context, R.drawable.map_marker)
                    } else {
                        null
                    },
                    onClick = {
                        onRoutePointSelected(point.id)
                        false
                    }
                )
            }

            if (uiState.routePoints.size > 1) {
                Polyline(
                    points = uiState.routePoints.map { it.location },
                    color = Color.Blue,
                    width = 5f
                )
            }

            uiState.userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Your Location",
                    icon = getBitmapDescriptorFromVector(
                        context,
                        android.R.drawable.ic_menu_mylocation
                    )
                )
            }
        }

        FloatingActionButton(
            onClick = onCloseMap,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Map"
            )
        }
    }
}

@Composable
fun RoutePointsList(
    routePoints: List<RoutePoint>,
    checkedInPoints: List<Long>,
    selectedRoutePointId: Long?,
    onRoutePointSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        itemsIndexed(routePoints) { index, point ->
            val isCheckedIn = checkedInPoints.contains(point.id)
            val isSelected = selectedRoutePointId == point.id

            RoutePointItem(
                routePoint = point,
                index = index,
                isCheckedIn = isCheckedIn,
                isSelected = isSelected,
                onClick = {
                    if (!isCheckedIn) {
                        onRoutePointSelected(point.id)
                    }
                }
            )

            if (index < routePoints.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun RoutePointItem(
    routePoint: RoutePoint,
    index: Int,
    isCheckedIn: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isCheckedIn -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = !isCheckedIn) { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isCheckedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCheckedIn) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Checked In",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Route point details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = routePoint.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            if (routePoint.description.isNotBlank()) {
                Text(
                    text = routePoint.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            routePoint.durationMinutes?.let { duration ->
                Text(
                    text = "Estimated duration: $duration min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isCheckedIn) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Checked In",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = if (isSelected) Icons.AutoMirrored.Filled.NavigateNext else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not Checked In",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CheckInCard(
    routePoint: RoutePoint,
    checkInNote: String,
    checkInImageUri: Uri?,
    isCheckingIn: Boolean,
    onNoteChanged: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onCheckInClicked: () -> Unit,
    onForceCheckInClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showForceCheckInDialog by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { onImageSelected(it) }
        }
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Check In to ${routePoint.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = checkInNote,
                onValueChange = onNoteChanged,
                label = { Text("Add a note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (checkInImageUri != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = rememberAsyncImagePainter(checkInImageUri),
                        contentDescription = "Check-in photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    IconButton(
                        onClick = { onImageSelected(Uri.EMPTY) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image"
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photo"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Photo (Optional)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Check-in button
            Button(
                onClick = onCheckInClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCheckingIn
            ) {
                if (isCheckingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking In...")
                } else {
                    Text("Check In")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Force check-in (hidden option for testing)
            TextButton(
                onClick = { showForceCheckInDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Having trouble checking in?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showForceCheckInDialog) {
        AlertDialog(
            onDismissRequest = { showForceCheckInDialog = false },
            title = { Text("Force Check-In") },
            text = {
                Text("If you're having trouble with location detection, you can force check-in. Note that this bypasses location verification.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onForceCheckInClicked()
                        showForceCheckInDialog = false
                    }
                ) {
                    Text("Force Check-In")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForceCheckInDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun Color.toBitmapDescriptor(context: Context, @DrawableRes drawableRes: Int): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, drawableRes)!!
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    drawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
        this.toArgb(),
        BlendModeCompat.SRC_ATOP
    )

    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun getBitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}