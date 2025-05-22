package com.xwurfel.tourry.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.xwurfel.tourry.R
import com.xwurfel.tourry.ui.theme.TourryTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class TourItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val price: Float,
    val isFree: Boolean,
    val duration: Int,
    val distance: Float,
    val startDateTime: LocalDateTime,
    val location: LatLng,
    val isLiveSoon: Boolean
)

enum class FilterType {
    DATE,
    DURATION,
    PRICE,
    DISTANCE
}

data class FilterState(
    val selectedDate: Long? = null,
    val durationRange: ClosedFloatingPointRange<Float> = 0f..180f,
    val priceRange: ClosedFloatingPointRange<Float> = 0f..1000f,
    val maxDistance: Float = 10f
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    MapsComposeExperimentalApi::class
)
@Composable
fun HomeScreenContent(
    onTourClick: (TourItem) -> Unit = {},
    onCreateTourClick: () -> Unit = {},
    onTourFavorite: (TourItem) -> Unit = {},
    onTourJoin: (TourItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMapView by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(FilterState()) }

    val tours = remember { generateDemoTours() }

    val filteredTours by remember(searchQuery, filterState) {
        derivedStateOf {
            tours.filter { tour ->
                // Search query filter
                val matchesQuery = searchQuery.isBlank() ||
                        tour.title.contains(searchQuery, ignoreCase = true) ||
                        tour.description.contains(searchQuery, ignoreCase = true)

                // Date filter
                val matchesDate = filterState.selectedDate == null ||
                        tour.startDateTime ==
                        Instant.ofEpochMilli(filterState.selectedDate!!)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                // Duration filter
                val matchesDuration = tour.duration >= filterState.durationRange.start &&
                        tour.duration <= filterState.durationRange.endInclusive

                // Price filter
                val matchesPrice = tour.price >= filterState.priceRange.start &&
                        tour.price <= filterState.priceRange.endInclusive

                // Distance filter
                val matchesDistance = tour.distance <= filterState.maxDistance

                matchesQuery && matchesDate && matchesDuration && matchesPrice && matchesDistance
            }
        }
    }

    val lvivCenter = LatLng(49.8397, 24.0297)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lvivCenter, 13f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                isMapView = isMapView,
                onToggleView = { isMapView = !isMapView },
                onOpenFilters = { showFilters = true },
                onOpenSearch = { showSearchBar = true }
            )

            AnimatedVisibility(
                visible = !isMapView,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ToursList(
                    tours = filteredTours,
                    onTourClick = onTourClick,
                    onTourFavorite = onTourFavorite,
                    onTourJoin = onTourJoin,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }

            AnimatedVisibility(
                visible = isMapView,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ToursMap(
                    tours = filteredTours,
                    cameraPositionState = cameraPositionState,
                    onMarkerClick = onTourClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        FloatingActionButton(
            onClick = onCreateTourClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(1f)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Tour")
        }
    }

    if (showSearchBar) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onDismiss = { showSearchBar = false }
        )
    }

    if (showFilters) {
        FilterBottomSheet(
            filterState = filterState,
            onFilterStateChanged = { filterState = it },
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
fun TopBar(
    isMapView: Boolean,
    onToggleView: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        }

        Text(
            text = "Explore Tours",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row {
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (isMapView) Icons.Outlined.ViewList else Icons.Outlined.Map,
                    contentDescription = if (isMapView) "List View" else "Map View"
                )
            }

            IconButton(onClick = onOpenFilters) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = "Filters"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Search Tours",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Enter tour name or description") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    onFilterStateChanged: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showDatePicker by remember { mutableStateOf(false) }
    var durationRange by remember { mutableStateOf(filterState.durationRange) }
    var priceRange by remember { mutableStateOf(filterState.priceRange) }
    var maxDistance by remember { mutableStateOf(filterState.maxDistance) }
    var selectedDate by remember { mutableStateOf(filterState.selectedDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Filter Tours",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Date Filter
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedDate != null) {
                            val date = Instant.ofEpochMilli(selectedDate!!)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } else {
                            "Select Date"
                        }
                    )
                }
                if (selectedDate != null) {
                    TextButton(
                        onClick = { selectedDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Clear")
                    }
                }
            }

            // Duration Range Filter
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${durationRange.start.toInt()} - ${durationRange.endInclusive.toInt()} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                RangeSlider(
                    value = durationRange,
                    onValueChange = { durationRange = it },
                    valueRange = 0f..180f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Price Range Filter
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${priceRange.start.toInt()} - ${priceRange.endInclusive.toInt()} UAH",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                RangeSlider(
                    value = priceRange,
                    onValueChange = { priceRange = it },
                    valueRange = 0f..1000f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Distance Filter
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Maximum Distance",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${maxDistance.toInt()} km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = maxDistance,
                    onValueChange = { maxDistance = it },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Apply Button
            Button(
                onClick = {
                    onFilterStateChanged(
                        FilterState(
                            selectedDate = selectedDate,
                            durationRange = durationRange,
                            priceRange = priceRange,
                            maxDistance = maxDistance
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Apply Filters")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ToursList(
    tours: List<TourItem>,
    onTourClick: (TourItem) -> Unit,
    onTourFavorite: (TourItem) -> Unit,
    onTourJoin: (TourItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier
    ) {
        items(
            items = tours,
            key = { it.id }
        ) { tour ->
            TourCard(
                tour = tour,
                onClick = { onTourClick(tour) },
                onFavorite = { onTourFavorite(tour) },
                onJoin = { onTourJoin(tour) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun ToursMap(
    tours: List<TourItem>,
    cameraPositionState: CameraPositionState,
    onMarkerClick: (TourItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Clustering(
                items = tours.map { ClusterItem(it) },
                onClusterClick = { _ -> false },
                onClusterItemClick = { item ->
                    onMarkerClick(item.tour)
                    false
                },
                clusterItemContent = { item ->
                    // Individual marker content
                }
            )
        }
    }
}

class ClusterItem(val tour: TourItem) : com.google.maps.android.clustering.ClusterItem {
    override fun getPosition(): LatLng = tour.location
    override fun getTitle(): String = tour.title
    override fun getSnippet(): String = tour.description
    override fun getZIndex(): Float = 1.0f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TourCard(
    tour: TourItem,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f
    var isFavorite by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX < -swipeThreshold -> {
                                onFavorite()
                                isFavorite = !isFavorite
                                offsetX = 0f
                            }

                            offsetX > swipeThreshold -> {
                                onJoin()
                                offsetX = 0f
                            }

                            else -> {
                                offsetX = 0f
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-200f, 200f)
                    }
                )
            }
    ) {
        if (offsetX < -20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Add to Favorites",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        } else if (offsetX > 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Join Tour",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX.dp, y = 0.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            Column {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tour.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = tour.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )

                    if (tour.isLiveSoon) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE SOON",
                                color = MaterialTheme.colorScheme.onTertiary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (tour.isFree) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (tour.isFree) "FREE" else "${tour.price.toInt()} UAH",
                            color = if (tour.isFree) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onTertiary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                            onFavorite()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Add to Favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tour.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notification),
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tour.rating.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tour.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TourInfoItem(
                            label = formatDuration(tour.duration),
                            icon = R.drawable.ic_notification
                        )

                        TourInfoItem(
                            label = "${tour.distance} km",
                            icon = R.drawable.ic_notification
                        )

                        TourInfoItem(
                            label = formatDate(tour.startDateTime),
                            icon = R.drawable.ic_notification
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TourInfoItem(
    label: String,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "$hours h ${if (mins > 0) "$mins m" else ""}" else "$mins m"
}

private fun formatDate(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM")
    return dateTime.format(formatter)
}

private fun generateDemoTours(): List<TourItem> {
    val titles = listOf(
        "Medieval Lviv",
        "High Castle Secrets",
        "Lychakiv Necropolis",
        "Austrian Lviv",
        "Armenian Quarter",
        "Jewish Heritage",
        "Town Hall Dungeons",
        "Lviv Coffee Tour"
    )

    val descriptions = listOf(
        "Journey through the historic center with a tour of medieval landmarks and architecture",
        "Walk through one of the highest points of the city with a panoramic view of Lviv",
        "Tour of the oldest and most famous necropolis in Lviv",
        "Discover architectural gems from the Austro-Hungarian Empire era",
        "Explore the rich cultural heritage of the Armenian community",
        "History of Lviv's Jewish community and tour of key locations",
        "Exciting tour of the medieval dungeons of the Town Hall",
        "Coffee tasting tour and history of the city's coffee tradition"
    )

    val imageUrls = listOf(
        "https://example.com/lviv1.jpg",
        "https://example.com/lviv2.jpg",
        "https://example.com/lviv3.jpg",
        "https://example.com/lviv4.jpg",
        "https://example.com/lviv5.jpg",
        "https://example.com/lviv6.jpg",
        "https://example.com/lviv7.jpg",
        "https://example.com/lviv8.jpg"
    )

    fun randomLatLng(): LatLng {
        val lvivCenterLat = 49.8397
        val lvivCenterLng = 24.0297
        val offset = Random.nextDouble(-0.02, 0.02)
        return LatLng(lvivCenterLat + offset, lvivCenterLng + offset)
    }

    return List(8) { index ->
        TourItem(
            id = "tour-$index",
            title = titles[index],
            description = descriptions[index],
            imageUrl = imageUrls[index],
            rating = 4.54f,
            price = if (Random.nextBoolean()) 0f else (Random.nextInt(10) + 1) * 50f,
            isFree = Random.nextBoolean(),
            duration = (Random.nextInt(5) + 1) * 30,
            distance = 5f,
            startDateTime = LocalDateTime.now().plusDays(Random.nextLong(14)),
            location = randomLatLng(),
            isLiveSoon = Random.nextBoolean()
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun HomeScreenContentPreview() {
    TourryTheme {
        Surface {
            HomeScreenContent()
        }
    }
}