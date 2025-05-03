package com.xwurfel.tourry.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.xwurfel.tourry.domain.user.model.UserRole
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent

@Composable
fun ProfileScreenRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToMyTours: () -> Unit,
    onNavigateToMyBookings: () -> Unit
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ProfileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onSaveProfile = viewModel::saveProfile,
        onNameChange = viewModel::onNameChange,
        onBioChange = viewModel::onBioChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onProfileImageSelected = viewModel::onProfileImageSelected,
        onTogglePendingBookings = viewModel::togglePendingBookingsExpanded,
        onToggleUpcomingBookings = viewModel::toggleUpcomingBookingsExpanded,
        onTogglePastBookings = viewModel::togglePastBookingsExpanded,
        onNavigateToMyTours = onNavigateToMyTours,
        onNavigateToMyBookings = onNavigateToMyBookings,
        onLogout = {
            viewModel.logout()
            onNavigateToLogin()
        })

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.user) {
        if (!uiState.isLoading && uiState.user == null) {
            onNavigateToLogin()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onProfileImageSelected: (Uri) -> Unit,
    onTogglePendingBookings: () -> Unit,
    onToggleUpcomingBookings: () -> Unit,
    onTogglePastBookings: () -> Unit,
    onNavigateToMyTours: () -> Unit,
    onNavigateToMyBookings: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("My Profile") }, actions = {
            if (!uiState.isEditing) {
                IconButton(onClick = onStartEditing) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                }
            }
            IconButton(onClick = { showLogoutConfirmDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingScreenContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.user == null -> {
                ErrorScreenContent(
                    errorMessage = "User not found",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeader(
                        profileImage = if (uiState.isEditing) uiState.editProfileImage else uiState.user.profileImageUri,
                        name = if (uiState.isEditing) uiState.editName else uiState.user.name,
                        role = uiState.user.role,
                        isEditing = uiState.isEditing,
                        onProfileImageSelected = onProfileImageSelected
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            if (uiState.isEditing) {
                                ProfileEditFields(
                                    name = uiState.editName,
                                    bio = uiState.editBio,
                                    phoneNumber = uiState.editPhoneNumber,
                                    onNameChange = onNameChange,
                                    onBioChange = onBioChange,
                                    onPhoneNumberChange = onPhoneNumberChange
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = onCancelEditing, modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = onSaveProfile, modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Save")
                                    }
                                }
                            } else {
                                ProfileInfoItem(
                                    icon = Icons.Default.Person,
                                    label = "Name",
                                    value = uiState.user.name
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )

                                ProfileInfoItem(
                                    icon = Icons.Default.Email,
                                    label = "Email",
                                    value = uiState.user.email
                                )

                                if (!uiState.user.bio.isNullOrBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    ProfileInfoItem(
                                        icon = Icons.Default.Person,
                                        label = "Bio",
                                        value = uiState.user.bio
                                    )
                                }

                                if (!uiState.user.phoneNumber.isNullOrBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    ProfileInfoItem(
                                        icon = Icons.Default.Phone,
                                        label = "Phone",
                                        value = uiState.user.phoneNumber
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    StatisticsCard(
                        userRole = uiState.user.role,
                        pendingBookingsCount = uiState.pendingBookingsCount,
                        upcomingBookingsCount = uiState.upcomingBookingsCount,
                        pastBookingsCount = uiState.pastBookingsCount,
                        totalToursCreated = uiState.totalToursCreated,
                        isPendingBookingsExpanded = uiState.isPendingBookingsExpanded,
                        isUpcomingBookingsExpanded = uiState.isUpcomingBookingsExpanded,
                        isPastBookingsExpanded = uiState.isPastBookingsExpanded,
                        onTogglePendingBookings = onTogglePendingBookings,
                        onToggleUpcomingBookings = onToggleUpcomingBookings,
                        onTogglePastBookings = onTogglePastBookings,
                        onNavigateToMyTours = onNavigateToMyTours,
                        onNavigateToMyBookings = onNavigateToMyBookings
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            onLogout()
                        }) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutConfirmDialog = false }) {
                        Text("Cancel")
                    }
                })
        }
    }
}

@Composable
fun ProfileHeader(
    profileImage: Uri?,
    name: String,
    role: UserRole,
    isEditing: Boolean,
    onProfileImageSelected: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
            uri?.let { onProfileImageSelected(it) }
        })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            if (profileImage != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImage),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isEditing) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Change Profile Picture",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (role) {
                UserRole.GUIDE -> "Tour Guide"
                UserRole.TOURIST -> "Tourist"
                UserRole.ADMIN -> "Administrator"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfileEditFields(
    name: String,
    bio: String,
    phoneNumber: String,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person, contentDescription = "Name"
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person, contentDescription = "Bio"
                )
            },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone, contentDescription = "Phone"
                )
            },
            singleLine = true
        )
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String
) {
    Row(
        verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value, style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun StatisticsCard(
    userRole: UserRole,
    pendingBookingsCount: Int,
    upcomingBookingsCount: Int,
    pastBookingsCount: Int,
    totalToursCreated: Int,
    isPendingBookingsExpanded: Boolean,
    isUpcomingBookingsExpanded: Boolean,
    isPastBookingsExpanded: Boolean,
    onTogglePendingBookings: () -> Unit,
    onToggleUpcomingBookings: () -> Unit,
    onTogglePastBookings: () -> Unit,
    onNavigateToMyTours: () -> Unit,
    onNavigateToMyBookings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Activity Summary",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // For all users - Bookings section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePendingBookings)
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Pending Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = pendingBookingsCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isPendingBookingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // Expandable content for pending bookings
            AnimatedVisibility(
                visible = isPendingBookingsExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(
                        300
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = if (pendingBookingsCount > 0) "You have $pendingBookingsCount pending booking(s)" else "No pending bookings",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onNavigateToMyBookings, enabled = pendingBookingsCount > 0
                    ) {
                        Text("View Bookings")
                    }
                }
            }

            HorizontalDivider()

            // Upcoming bookings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleUpcomingBookings)
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Upcoming Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = upcomingBookingsCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isUpcomingBookingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // Expandable content for upcoming bookings
            AnimatedVisibility(
                visible = isUpcomingBookingsExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(
                        300
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = if (upcomingBookingsCount > 0) "You have $upcomingBookingsCount upcoming tour(s)" else "No upcoming tours",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onNavigateToMyBookings, enabled = upcomingBookingsCount > 0
                    ) {
                        Text("View Bookings")
                    }
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePastBookings)
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Past Bookings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = pastBookingsCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isPastBookingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // Expandable content for past bookings
            AnimatedVisibility(
                visible = isPastBookingsExpanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(
                        300
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = if (pastBookingsCount > 0) "You have completed $pastBookingsCount tour(s)" else "No past tours",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onNavigateToMyBookings, enabled = pastBookingsCount > 0
                    ) {
                        Text("View History")
                    }
                }
            }

            if (userRole == UserRole.GUIDE) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Created Tours",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = totalToursCreated.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = onNavigateToMyTours, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage My Tours")
                }
            }
        }
    }
}