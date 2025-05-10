package com.xwurfel.tourry.ui.tourbuilder.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentEditor(
    content: ContentDraft? = null,
    onSave: (String, String, ContentType, String?) -> Unit,
    onCancel: () -> Unit,
    onRequestMediaUpload: (ContentType) -> Unit,
    onDelete: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val isNewContent = content == null
    var title by remember { mutableStateOf(content?.title ?: "") }
    var description by remember { mutableStateOf(content?.description ?: "") }
    var selectedType by remember { mutableStateOf(content?.type ?: ContentType.TEXT) }
    var mediaUrl by remember { mutableStateOf(content?.mediaUrl ?: "") }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (isNewContent) "Add Content" else "Edit Content",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content type selection
            Text(
                text = "Content Type",
                style = MaterialTheme.typography.titleSmall
            )

            Column {
                ContentType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (type == selectedType),
                                onClick = { selectedType = type }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (type == selectedType),
                            onClick = null // null because the row is already clickable
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = when (type) {
                                ContentType.TEXT -> "Text"
                                ContentType.IMAGE -> "Image"
                                ContentType.AUDIO -> "Audio"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

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

            // Show different UI based on content type
            when (selectedType) {
                ContentType.TEXT -> {
                    // Text content doesn't need additional fields
                }

                ContentType.IMAGE, ContentType.AUDIO -> {
                    if (mediaUrl.isNotBlank()) {
                        // Display media info if already uploaded
                        Text(
                            text = "Media: $mediaUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Upload button
                    Button(
                        onClick = { onRequestMediaUpload(selectedType) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (mediaUrl.isBlank()) "Upload ${selectedType.name.lowercase()}"
                            else "Replace ${selectedType.name.lowercase()}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isNewContent && onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Content")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save and Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSave(
                            title,
                            description,
                            selectedType,
                            mediaUrl.ifBlank { null })
                    },
                    enabled = title.isNotBlank() &&
                            (selectedType == ContentType.TEXT || mediaUrl.isNotBlank())
                ) {
                    Text("Save")
                }
            }
        }
    }
}