package com.xwurfel.tourry.presentation.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import java.time.format.DateTimeFormatter

@Composable
fun BookingScreenRoute(
    tourId: Long, onBackClicked: () -> Unit, onBookingComplete: () -> Unit
) {
    val viewModel: BookingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tourId) {
        viewModel.loadTourInfo(tourId)
    }

    LaunchedEffect(uiState.isBookingCompleted) {
        if (uiState.isBookingCompleted) {
            onBookingComplete()
        }
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

        uiState.tour == null -> {
            ErrorScreenContent(
                errorMessage = "Tour not found", modifier = Modifier.fillMaxSize()
            )
        }

        else -> {
            BookingScreen(
                uiState = uiState,
                onBackClicked = onBackClicked,
                onParticipantsChanged = viewModel::onParticipantsChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onBookTourClicked = viewModel::bookTour
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    uiState: BookingUiState,
    onBackClicked: () -> Unit,
    onParticipantsChanged: (Int) -> Unit,
    onNotesChanged: (String) -> Unit,
    onBookTourClicked: () -> Unit
) {
    val tour = uiState.tour ?: return
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Book Tour") }, navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = tour.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Date: ${tour.startDateTime.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Meeting Point: ${tour.meetingPointAddress}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Price: ${tour.price} per person",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Booking Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Number of Participants",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Text(
                            text = "1", style = MaterialTheme.typography.bodyMedium
                        )

                        Slider(
                            value = uiState.numberOfParticipants.toFloat(),
                            onValueChange = { onParticipantsChanged(it.toInt()) },
                            valueRange = 1f..uiState.maxParticipants.toFloat(),
                            steps = uiState.maxParticipants - 2,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = uiState.maxParticipants.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "${uiState.numberOfParticipants} participants",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (uiState.maxParticipants < uiState.tour.capacity) {
                        Text(
                            text = "Note: Maximum ${uiState.maxParticipants} participants available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Special Requests or Notes (Optional)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = onNotesChanged,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Total Price", style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${uiState.totalPrice}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "(${uiState.numberOfParticipants} × ${tour.price})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onBookTourClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isLoading && uiState.errorMessage == null
            ) {
                Text(
                    text = "Book Now",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.bookingError != null) {
                Text(
                    text = uiState.bookingError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}