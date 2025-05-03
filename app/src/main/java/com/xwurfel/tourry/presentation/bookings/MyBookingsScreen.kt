package com.xwurfel.tourry.presentation.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.presentation.common.ErrorDialogContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import com.xwurfel.tourry.util.extensions.capitalizeFirstLetter
import java.time.format.DateTimeFormatter

@Composable
fun MyBookingsScreenRoute(
    onNavigateToTourDetails: (Long) -> Unit
) {
    val viewModel: MyBookingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    MyBookingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::loadBookings,
        onCancelBooking = viewModel::cancelBooking,
        onSelectTab = viewModel::selectTab,
        onNavigateToTourDetails = onNavigateToTourDetails
    )

    // Handle success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Handle error messages
    if (uiState.errorMessage != null) {
        ErrorDialogContent(
            errorMessage = uiState.errorMessage ?: "", onDismiss = viewModel::clearErrorMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    uiState: MyBookingsUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onCancelBooking: (Long) -> Unit,
    onSelectTab: (BookingsTab) -> Unit,
    onNavigateToTourDetails: (Long) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("My Bookings") }, actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal
            ) {
                BookingsTab.entries.forEach { tab ->
                    val count = when (tab) {
                        BookingsTab.PENDING -> uiState.pendingBookings.size
                        BookingsTab.UPCOMING -> uiState.upcomingBookings.size
                        BookingsTab.PAST -> uiState.pastBookings.size
                    }

                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text("${tab.name.lowercase().capitalizeFirstLetter()} ($count)")
                        })
                }
            }

            if (uiState.isLoading) {
                LoadingScreenContent(modifier = Modifier.weight(1f))
            } else {
                val bookings = when (uiState.selectedTab) {
                    BookingsTab.PENDING -> uiState.pendingBookings
                    BookingsTab.UPCOMING -> uiState.upcomingBookings
                    BookingsTab.PAST -> uiState.pastBookings
                }

                if (bookings.isEmpty()) {
                    EmptyBookingsList(modifier = Modifier.weight(1f))
                } else {
                    BookingsList(
                        bookings = bookings,
                        onCancelBooking = onCancelBooking,
                        onNavigateToTourDetails = onNavigateToTourDetails,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BookingsList(
    bookings: List<BookingWithTour>,
    onCancelBooking: (Long) -> Unit,
    onNavigateToTourDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(bookings) { bookingWithTour ->
            BookingCard(
                bookingWithTour = bookingWithTour,
                onCancelBooking = onCancelBooking,
                onNavigateToTourDetails = onNavigateToTourDetails
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BookingCard(
    bookingWithTour: BookingWithTour,
    onCancelBooking: (Long) -> Unit,
    onNavigateToTourDetails: (Long) -> Unit
) {
    val booking = bookingWithTour.booking
    val tour = bookingWithTour.tour
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tour Image or Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (tour.imageUri != null) {
                    AsyncImage(
                        model = tour.imageUri,
                        contentDescription = tour.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tour.title.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = when (booking.status) {
                                BookingStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
                                BookingStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
                                BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
                            }.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (booking.status) {
                                BookingStatus.PENDING -> Icons.Default.Person
                                BookingStatus.CONFIRMED -> Icons.Default.CheckCircle
                                BookingStatus.COMPLETED -> Icons.Default.CheckCircle
                                BookingStatus.CANCELLED -> Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = booking.status.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Booking Details
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = tour.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tour.startDateTime.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tour.meetingPointAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Booking info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Participants",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${booking.numberOfParticipants} participants",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Total price
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Total: ${booking.totalPrice}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { onNavigateToTourDetails(tour.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Tour")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                        Button(
                            onClick = { showCancelDialog = true }, modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking") },
            text = { Text("Are you sure you want to cancel this booking?") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(booking.id)
                        showCancelDialog = false
                    }) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false }) {
                    Text("No, Keep Booking")
                }
            })
    }
}

@Composable
fun EmptyBookingsList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No bookings found", style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Book a tour to see it listed here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}