package com.xwurfel.tourry.presentation.poi_details

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.poi.model.PointOfInterest
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class PoiDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val poiRepository: PoiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoiDetailsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    fun onIdChanged(id: Long) {
        viewModelScope.launch {
            try {
                poiRepository.getPoiById(id).collect { poi ->
                    val imageUri = retrieveImageFromInternalStorageById(id)
                    _uiState.update {
                        it.copy(
                            poi = poi?.copy(imageUri = imageUri),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
        _uiState.update { it.copy(initialId = id, isLoading = false) }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(poi = it.poi?.copy(title = newTitle)) }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update { it.copy(poi = it.poi?.copy(description = newDescription)) }
    }

    fun onImageSelected(imageUri: Uri) {
        viewModelScope.launch {
            val copiedUri = copyImageToInternalStorage(imageUri)
            _uiState.update { it.copy(poi = it.poi?.copy(imageUri = copiedUri)) }
        }
    }

    private suspend fun copyImageToInternalStorage(imageUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = getApplication(context).contentResolver.openInputStream(imageUri)
                val fileName = "poi_image_${_uiState.value.poi?.id}.jpg"
                val file = File(getApplication(context).filesDir, fileName)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                return@withContext Uri.fromFile(file)
            } catch (e: Exception) {
                ensureActive()
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun retrieveImageFromInternalStorageById(poiId: Long): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "poi_image_$poiId.jpg"
                val file = File(getApplication(context).filesDir, fileName)
                return@withContext if (file.exists()) Uri.fromFile(file) else null
            } catch (e: Exception) {
                ensureActive()
                e.printStackTrace()
                null
            }
        }
    }


    fun savePoi() {
        val currentState = _uiState.value
        val poi = currentState.poi
        poi?.let {
            viewModelScope.launch {
                try {
                    poiRepository.savePoi(poi)
                    _uiState.update { it.copy(isUpdated = true) }
                } catch (e: Exception) {
                    ensureActive()
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            }
        }
    }

    fun deletePoi() {
        viewModelScope.launch {
            try {
                _uiState.value.poi?.let { poi ->
                    poiRepository.deletePoi(poi.id)
                    _uiState.update { it.copy(isDeleted = true) }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onImageRemoved() {
        viewModelScope.launch {
            _uiState.value.poi?.let { poi ->
                poi.imageUri?.let { uri ->
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        file.delete()
                    }
                }
                _uiState.update { it.copy(poi = poi.copy(imageUri = null)) }
            }
        }
    }

    data class PoiDetailsUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val initialId: Long = 0,
        val poi: PointOfInterest? = null,
        val isUpdated: Boolean = false,
        val isDeleted: Boolean = false,
    )
}