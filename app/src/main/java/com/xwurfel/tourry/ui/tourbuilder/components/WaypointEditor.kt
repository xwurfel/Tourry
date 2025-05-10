package com.xwurfel.tourry.ui.tourbuilder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointEditor(
    waypoint: WaypointDraft,
    onSave: (String, String, Int, Float) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(waypoint.title) }
    var description by remember { mutableStateOf(waypoint.description) }
    var durationMinutes by remember { mutableStateOf(waypoint.durationMinutes.toString()) }
    var geofenceRadius by remember { mutableStateOf(waypoint.geofenceRadius.toString()) }

    var durationError by remember { mutableStateOf<String?>(null) }
    var radiusError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Edit Waypoint",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration field
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = {
                    durationMinutes = it
                    durationError = try {
                        val minutes = it.toInt()
                        if (minutes < 1) "Must be at least 1 minute" else null
                    } catch (e: NumberFormatException) {
                        "Must be a number"
                    }
                },
                label = { Text("Duration (minutes)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                isError = durationError != null,
                supportingText = {
                    durationError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Geofence radius field
            OutlinedTextField(
                value = geofenceRadius,
                onValueChange = {
                    geofenceRadius = it
                    radiusError = try {
                        val radius = it.toFloat()
                        if (radius < 10f) "Must be at least 10 meters" else null
                    } catch (e: NumberFormatException) {
                        "Must be a number"
                    }
                },
                label = { Text("Geofence Radius (meters)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                isError = radiusError != null,
                supportingText = {
                    radiusError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location information (read-only)
            Text(
                text = "Location: ${
                    String.format(
                        "%.6f, %.6f",
                        waypoint.position.latitude,
                        waypoint.position.longitude
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Delete button
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }

                // Save and Cancel buttons
                Row {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSave(
                                title,
                                description,
                                durationMinutes.toIntOrNull() ?: 30,
                                geofenceRadius.toFloatOrNull() ?: 50f
                            )
                        },
                        enabled = title.isNotBlank() && durationError == null && radiusError == null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}