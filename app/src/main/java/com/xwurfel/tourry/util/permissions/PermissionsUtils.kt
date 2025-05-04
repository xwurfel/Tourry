package com.xwurfel.tourry.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this, permission
    ) == PackageManager.PERMISSION_GRANTED
}


fun Activity.shouldShowPermissionRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}


fun Context.hasGeofencingPermissions(): Boolean {
    val hasFineLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        true // Not required for Android < 10
    }

    return hasFineLocation && hasBackgroundLocation
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

@Composable
fun LocationPermissionsHandler(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionsGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context is Activity && context.shouldShowPermissionRationale(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                showRationaleDialog = true
            } else {
                showSettingsDialog = true
            }
        }
    }

    val fineLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                onPermissionsGranted()
            }
        } else {
            if (context is Activity && context.shouldShowPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showRationaleDialog = true
            } else {
                showSettingsDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (context.hasGeofencingPermissions()) {
            onPermissionsGranted()
        } else if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            backgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Location Permission Required") },
            text = {
                Column {
                    Text(
                        "This app needs location permission to notify you when you're near tour points of interest."
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Background location permission is needed to detect when you're near " + "tour stops, even when the app is not in use."
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            fineLocationPermissionLauncher.launch(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !context.hasPermission(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        ) {
                            backgroundLocationPermissionLauncher.launch(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
                    }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Maybe Later")
                }
            })
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Required") },
            text = {
                Text(
                    "Location permission is required for geofencing features. " + "Please enable it in app settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        context.openAppSettings()
                    }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Not Now")
                }
            })
    }
}