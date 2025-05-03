package com.xwurfel.tourry.presentation.tour.edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.xwurfel.tourry.presentation.common.ErrorDialogContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditTourScreenRoute(
    tourId: Long? = null, onNavigateBack: () -> Unit
) {
    val viewModel: EditTourViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadTour(tourId)
    }

    EditTourScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTitleChanged = viewModel::onTitleChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onImageSelected = viewModel::onImageSelected,
        onMeetingPointAddressChanged = viewModel::onMeetingPointAddressChanged,
        onStartDateTimeChanged = viewModel::onStartDateTimeChanged,
        onEndDateTimeChanged = viewModel::onEndDateTimeChanged,
        onPriceChanged = { viewModel.onPriceChanged(it.toDoubleOrNull() ?: 0.0) },
        onCapacityChanged = { viewModel.onCapacityChanged(it.toIntOrNull() ?: 1) },
        onCategorySelected = viewModel::onCategorySelected,
        onSaveClicked = viewModel::saveTour,
        onNavigateBack = onNavigateBack
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

    // Navigate back if saved successfully
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTourScreen(
    uiState: EditTourUiState,
    snackbarHostState: SnackbarHostState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onMeetingPointAddressChanged: (String) -> Unit,
    onStartDateTimeChanged: (LocalDateTime) -> Unit,
    onEndDateTimeChanged: (LocalDateTime) -> Unit,
    onPriceChanged: (String) -> Unit,
    onCapacityChanged: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSaveClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = {
            Text(if (uiState.isNewTour) "Create Tour" else "Edit Tour")
        }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        if (uiState.isLoading) {
            LoadingScreenContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Tour Image
                TourImageSelector(
                    imageUri = uiState.imageUri, onImageSelected = onImageSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tour Details Form
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tour Details",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Title
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = onTitleChanged,
                            label = { Text("Title") },
                            placeholder = { Text("Enter tour title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = onDescriptionChanged,
                            label = { Text("Description") },
                            placeholder = { Text("Enter tour description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Meeting point
                        OutlinedTextField(
                            value = uiState.meetingPointAddress,
                            onValueChange = onMeetingPointAddressChanged,
                            label = { Text("Meeting Point") },
                            placeholder = { Text("Enter meeting point address") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location"
                                )
                            })

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date and Time Pickers
                        DateTimePicker(
                            label = "Start Date & Time",
                            dateTime = uiState.startDateTime,
                            onDateTimeChanged = onStartDateTimeChanged
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DateTimePicker(
                            label = "End Date & Time",
                            dateTime = uiState.endDateTime,
                            onDateTimeChanged = onEndDateTimeChanged
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Price
                        OutlinedTextField(
                            value = if (uiState.price == 0.0) "" else uiState.price.toString(),
                            onValueChange = onPriceChanged,
                            label = { Text("Price") },
                            placeholder = { Text("Enter price per person") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Capacity
                        OutlinedTextField(
                            value = if (uiState.capacity == 0) "" else uiState.capacity.toString(),
                            onValueChange = onCapacityChanged,
                            label = { Text("Capacity") },
                            placeholder = { Text("Enter maximum number of participants") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Capacity"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category selection
                        CategoryDropdown(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategoryId,
                            onCategorySelected = onCategorySelected
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Save button
                        Button(
                            onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isNewTour) "Create Tour" else "Save Changes")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TourImageSelector(
    imageUri: Uri?, onImageSelected: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
            uri?.let { onImageSelected(it) }
        })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }, contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Tour Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Semi-transparent overlay to make icon visible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )

            // Add a change image button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Change Tour Image",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Add Tour Image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add Tour Image", color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    label: String, dateTime: LocalDateTime, onDateTimeChanged: (LocalDateTime) -> Unit
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    // Click handler for date picker
    val datePickerDialog = DatePickerDialog(
        context, { _, year, month, dayOfMonth ->
            // After date is selected, show time picker
            val timePickerDialog = TimePickerDialog(
                context, { _, hourOfDay, minute ->
                    // Create new LocalDateTime with selected date and time
                    val newDateTime = LocalDateTime.of(
                        year, month + 1, dayOfMonth, hourOfDay, minute
                    )
                    onDateTimeChanged(newDateTime)
                }, dateTime.hour, dateTime.minute, true // 24-hour format
            )
            timePickerDialog.show()
        }, dateTime.year, dateTime.monthValue - 1, // Month is 0-based in DatePickerDialog
        dateTime.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dateTime.format(formatter),
            onValueChange = { /* Read-only field */ },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday, contentDescription = "Calendar"
                )
            },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date and Time")
                }
            })

        Text(
            text = "Tap on the calendar icon to select date and time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<com.xwurfel.tourry.domain.category.model.TourCategory>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == selectedCategoryId }
        ?: if (categories.isNotEmpty()) categories.first() else null

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Select Category",
            onValueChange = { },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Category, contentDescription = "Category"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text("Category") })

        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = {
                    onCategorySelected(category.id)
                    expanded = false
                })
            }
        }
    }
}