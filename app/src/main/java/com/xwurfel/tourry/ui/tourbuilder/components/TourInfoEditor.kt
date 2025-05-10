package com.xwurfel.tourry.ui.tourbuilder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.feature.discovery.domain.model.TourCategory
import com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty
import com.xwurfel.tourry.feature.tourbuilder.domain.model.TourDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourInfoEditor(
    tourDraft: TourDraft,
    onSave: (String, String, String, TourCategory, TourDifficulty, Double?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(tourDraft.title) }
    var description by remember { mutableStateOf(tourDraft.description) }
    var location by remember { mutableStateOf(tourDraft.location) }
    var category by remember { mutableStateOf(tourDraft.category) }
    var difficulty by remember { mutableStateOf(tourDraft.difficulty) }
    var price by remember { mutableStateOf(tourDraft.price?.toString() ?: "") }
    var isPublic by remember { mutableStateOf(tourDraft.isPublic) }

    var priceError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Tour Title") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TourCategory.entries.size) { index ->
                val tourCategory = TourCategory.entries[index]
                FilterChip(
                    selected = category == tourCategory,
                    onClick = { category = tourCategory },
                    label = {
                        Text(tourCategory.name.lowercase().capitalize())
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Difficulty",
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            TourDifficulty.entries.forEach { difficultyOption ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = difficulty == difficultyOption,
                            onClick = { difficulty = difficultyOption }
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = difficulty == difficultyOption,
                        onClick = null
                    )
                    Text(
                        text = difficultyOption.name.lowercase().capitalize(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = price,
            onValueChange = {
                price = it
                priceError = try {
                    if (it.isNotBlank()) {
                        val priceValue = it.toDouble()
                        if (priceValue < 0) "Price cannot be negative" else null
                    } else {
                        null
                    }
                } catch (_: NumberFormatException) {
                    "Must be a valid number"
                }
            },
            label = { Text("Price (leave empty for free)") },
            singleLine = true,
            prefix = { Text("$") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            isError = priceError != null,
            supportingText = {
                priceError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Public/Private toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tour Visibility:",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isPublic,
                onCheckedChange = { isPublic = it }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isPublic) "Public" else "Private",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save button
        Button(
            onClick = {
                onSave(
                    title,
                    description,
                    location,
                    category,
                    difficulty,
                    price.toDoubleOrNull(),
                    isPublic
                )
            },
            enabled = title.isNotBlank() && description.isNotBlank() && location.isNotBlank() && priceError == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Tour Info")
        }
    }
}

// Helper extension
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}