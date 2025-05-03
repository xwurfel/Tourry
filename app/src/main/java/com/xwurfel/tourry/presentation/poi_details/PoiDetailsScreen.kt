package com.xwurfel.tourry.presentation.poi_details

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent
import com.xwurfel.tourry.presentation.save_poi.ImagePicker

@Composable
fun PoiDetailsScreenRoute(poiId: Long, onBack: () -> Unit) {
    val viewModel: PoiDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            LoadingScreenContent(Modifier.fillMaxSize())
        }

        uiState.errorMessage != null -> {
            ErrorScreenContent(uiState.errorMessage ?: "", Modifier.fillMaxSize())
        }

        else -> {
            PoiDetailsScreen(
                uiState,
                onTitleChanged = viewModel::onTitleChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onImageSelected = viewModel::onImageSelected,
                onSaveClicked = viewModel::savePoi,
                onDeleteClicked = viewModel::deletePoi,
                onBackClicked = onBack,
                onImageRemoved = viewModel::onImageRemoved,
            )
        }
    }

    LaunchedEffect(uiState.isUpdated, uiState.isDeleted) {
        if (uiState.isUpdated || uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIdChanged(poiId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailsScreen(
    uiState: PoiDetailsViewModel.PoiDetailsUiState,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onImageSelected: (Uri) -> Unit = {},
    onImageRemoved: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onDeleteClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {},
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Point of Interest Details") }, navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }, content = { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.poi?.title ?: "",
                            onValueChange = onTitleChanged,
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.poi?.description ?: "",
                            onValueChange = onDescriptionChanged,
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5,
                            singleLine = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Image", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.poi?.imageUri != null) {
                            Box {
                                Image(
                                    painter = rememberAsyncImagePainter(uiState.poi.imageUri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                )
                                IconButton(
                                    onClick = onImageRemoved,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = Color.White
                                    )
                                }
                            }
                        } else {
                            ImagePicker(
                                onImageSelected
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ElevatedButton(
                                onClick = onSaveClicked, modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            ElevatedButton(
                                onClick = onDeleteClicked,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                        uiState.errorMessage?.let { errorMessage ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    })
}

@Preview
@Composable
private fun PreviewPoiDetailsScreen() {
    PoiDetailsScreen(PoiDetailsViewModel.PoiDetailsUiState())
}