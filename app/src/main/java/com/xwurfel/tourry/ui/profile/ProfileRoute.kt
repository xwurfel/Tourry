package com.xwurfel.tourry.ui.profile

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.extension.collectWithLifecycle
import com.xwurfel.tourry.ui.profile.components.ProfileEditDialog

@Composable
fun ProfileRoute(
    onNavigateToAuth: () -> Unit,
    onNavigateToTourCreation: () -> Unit,
    onNavigateToMyTours: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    viewModel.event.collectWithLifecycle { event ->
        when (event) {
            ProfileEvent.NavigateToAuth -> onNavigateToAuth()
            ProfileEvent.NavigateToTourCreation -> onNavigateToTourCreation()
            ProfileEvent.NavigateToMyTours -> onNavigateToMyTours()
            ProfileEvent.NavigateToEditProfile -> {
                // Handled by showEditDialog state
            }

            ProfileEvent.NavigateToAnalytics -> {
                // TODO: Navigate to analytics screen when implemented
            }

            ProfileEvent.NavigateToHelp -> {
                // TODO: Navigate to help screen when implemented
            }

            is ProfileEvent.ShareProfile -> {
                // Create share intent
                val shareText =
                    "Check out ${event.user.name}'s profile on Tourry! They've created ${event.user.stats?.toursCreated ?: 0} tours and joined ${event.user.stats?.toursJoined ?: 0} experiences."
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
            }
        }
    }

    // Show success/error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(uiState.isProfileUpdated) {
        if (uiState.isProfileUpdated) {
            snackbarHostState.showSnackbar("Profile updated successfully!")
        }
    }

    ProfileScreen(
        uiState = uiState,
        onIntent = viewModel::acceptIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_profile)) },
                actions = {
                    IconButton(
                        onClick = { onIntent(ProfileIntent.RefreshProfile) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (uiState.isLoading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState.user != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }

                        IconButton(onClick = { onIntent(ProfileIntent.ShareProfile) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Profile")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.user != null) {
                ExtendedFloatingActionButton(
                    onClick = { onIntent(ProfileIntent.CreateTour) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create Tour") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.user == null) {
            LoadingProfileSection(paddingValues)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(bottom = 88.dp) // Account for FAB
            ) {
                if (uiState.user != null) {
                    // Authenticated user content
                    UserProfileSection(
                        user = uiState.user,
                        onEditClick = { showEditDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced Stats section
                    UserStatsSection(
                        stats = uiState.userStats,
                        onMyToursClick = { onIntent(ProfileIntent.ViewMyTours) },
                        onCreateTourClick = { onIntent(ProfileIntent.CreateTour) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Actions
                    QuickActionsSection(onIntent = onIntent)

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Guest user section
                    GuestProfileSection(
                        onSignInClick = { onIntent(ProfileIntent.SignIn) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Settings and options
                SettingsSection(
                    isSignedIn = uiState.user != null,
                    notificationsEnabled = uiState.notificationsEnabled,
                    locationSharingEnabled = uiState.locationSharingEnabled,
                    onIntent = onIntent,
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteAccountClick = { showDeleteDialog = true }
                )
            }
        }
    }

    // Dialogs
    ProfileDialogs(
        uiState = uiState,
        showEditDialog = showEditDialog,
        showDeleteDialog = showDeleteDialog,
        showSignOutDialog = showSignOutDialog,
        onEditDialogDismiss = { showEditDialog = false },
        onDeleteDialogDismiss = { showDeleteDialog = false },
        onSignOutDialogDismiss = { showSignOutDialog = false },
        onIntent = onIntent
    )
}

@Composable
private fun LoadingProfileSection(paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Loading your profile...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UserProfileSection(
    user: UserProfile,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture with loading state
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    if (user.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (user.email.isNotEmpty()) {
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (user.bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (user.rating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${
                                    String.format(
                                        "%.1f",
                                        user.rating
                                    )
                                } • ${user.reviewsCount} reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatsSection(
    stats: UserStats?,
    onMyToursClick: () -> Unit,
    onCreateTourClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                TextButton(onClick = onMyToursClick) {
                    Text("View All")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stats != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Tours Created",
                        value = stats.toursCreated.toString(),
                        icon = Icons.Default.Add,
                        onClick = onCreateTourClick
                    )
                    StatItem(
                        label = "Tours Joined",
                        value = stats.toursJoined.toString(),
                        icon = Icons.Default.Tour,
                        onClick = onMyToursClick
                    )
                    StatItem(
                        label = "Total Participants",
                        value = stats.totalParticipants.toString(),
                        icon = Icons.Default.Person,
                        onClick = null
                    )
                }
            } else {
                // Loading skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier
        } else {
            Modifier
        }
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = onClick ?: {}
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickActionsSection(onIntent: (ProfileIntent) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedCard {
            Column {
                ListItem(
                    headlineContent = { Text("Create New Tour") },
                    supportingContent = { Text("Share your favorite places with others") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("View My Tours") },
                    supportingContent = { Text("Manage your created and joined tours") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Tour,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GuestProfileSection(onSignInClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Welcome to Tourry!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Sign in to create tours, join experiences, and track your adventures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSignInClick) {
                Text("Sign In")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSection(
    isSignedIn: Boolean,
    notificationsEnabled: Boolean,
    locationSharingEnabled: Boolean,
    onIntent: (ProfileIntent) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card {
            Column {
                // Notifications Setting
                ListItem(
                    headlineContent = { Text("Notifications") },
                    supportingContent = { Text("Get notified about tour updates and messages") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                onIntent(ProfileIntent.UpdateNotificationSettings(enabled))
                            }
                        )
                    }
                )

                if (isSignedIn) {
                    HorizontalDivider()

                    // Location Sharing Setting
                    ListItem(
                        headlineContent = { Text("Location Sharing") },
                        supportingContent = { Text("Share your location during tours for safety") },
                        leadingContent = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = locationSharingEnabled,
                                onCheckedChange = { enabled ->
                                    onIntent(ProfileIntent.UpdateLocationSharing(enabled))
                                }
                            )
                        }
                    )

                    HorizontalDivider()

                    // Account Settings
                    ListItem(
                        headlineContent = { Text("Account Settings") },
                        supportingContent = { Text("Manage your account preferences") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Analytics
                    ListItem(
                        headlineContent = { Text("Analytics") },
                        supportingContent = { Text("View your tour statistics and insights") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                // Help & Support
                ListItem(
                    headlineContent = { Text("Help & Support") },
                    supportingContent = { Text("Get help or contact support") },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.Help,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSignedIn) {
                    HorizontalDivider()

                    // Sign Out
                    ListItem(
                        headlineContent = {
                            Text(
                                "Sign Out",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                "Sign out of your account",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Delete Account
                    ListItem(
                        headlineContent = {
                            Text(
                                "Delete Account",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                "Permanently delete your account and all data",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDialogs(
    uiState: ProfileUiState,
    showEditDialog: Boolean,
    showDeleteDialog: Boolean,
    showSignOutDialog: Boolean,
    onEditDialogDismiss: () -> Unit,
    onDeleteDialogDismiss: () -> Unit,
    onSignOutDialogDismiss: () -> Unit,
    onIntent: (ProfileIntent) -> Unit
) {
    // Profile Edit Dialog
    if (showEditDialog && uiState.user != null) {
        ProfileEditDialog(
            user = uiState.user,
            isUpdating = uiState.isUpdatingProfile,
            isUploadingAvatar = uiState.isUploadingAvatar,
            onDismiss = onEditDialogDismiss,
            onSave = { updatedProfile ->
                onIntent(ProfileIntent.UpdateProfile(updatedProfile))
                onEditDialogDismiss()
            },
            onUploadAvatar = { imageUri ->
                onIntent(ProfileIntent.UploadAvatar(imageUri))
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = onSignOutDialogDismiss,
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? You can always sign back in later.") },
            confirmButton = {
                Button(
                    onClick = {
                        onSignOutDialogDismiss()
                        onIntent(ProfileIntent.SignOut)
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = onSignOutDialogDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDialogDismiss,
            title = { Text("Delete Account") },
            text = {
                Column {
                    Text("This action cannot be undone. All your data will be permanently deleted including:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Your profile and account information")
                    Text("• All tours you've created")
                    Text("• Your tour history and statistics")
                    Text("• All reviews and ratings")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDialogDismiss()
                        onIntent(ProfileIntent.DeleteAccount)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !uiState.isDeletingAccount
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDialogDismiss,
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}