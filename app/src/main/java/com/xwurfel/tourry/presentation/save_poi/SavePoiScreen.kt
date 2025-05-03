package com.xwurfel.tourry.presentation.save_poi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.xwurfel.tourry.presentation.common.ErrorScreenContent
import com.xwurfel.tourry.presentation.common.LoadingScreenContent

@Composable
fun PoiSettingsScreenRoute(
    latitude: Double,
    longitude: Double,
    onBack: () -> Unit,
) {
    val viewModel: SavePoiViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            LoadingScreenContent(Modifier.fillMaxSize())
        }

        uiState.errorMessage != null -> {
            ErrorScreenContent(uiState.errorMessage ?: "", Modifier.fillMaxSize())
        }

        else -> {
            PoiSettingsScreen(
                uiState = uiState,
                onTitleChanged = viewModel::onTitleChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onImageSelected = viewModel::onImageSelected,
                onSaveClicked = viewModel::savePoi
            )
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setInitialLocation(latitude, longitude)
    }
}

@Composable
fun PoiSettingsScreen(
    uiState: SavePoiViewModel.PoiSettingsUiState,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onImageSelected: (Uri) -> Unit = {},
    onSaveClicked: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        TextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = uiState.description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        ImagePicker(onImageSelected = onImageSelected)
        Spacer(modifier = Modifier.height(8.dp))

        uiState.imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onSaveClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        uiState.errorMessage?.let { errorMessage ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ImagePicker(
    onImageSelected: (Uri) -> Unit,
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { onImageSelected(it) }
        }
    )

    Button(onClick = {
        pickImageLauncher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
        )
    }) {
        Text("Select Image")
    }
}


@Preview
@Composable
private fun PreviewPoiSettingsScreen() {
    PoiSettingsScreen(SavePoiViewModel.PoiSettingsUiState())
}