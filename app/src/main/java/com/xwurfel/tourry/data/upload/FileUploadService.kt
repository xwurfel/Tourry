package com.xwurfel.tourry.data.upload

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.xwurfel.tourry.data.network.api.FileUploadApi
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUploadService @Inject constructor(
    private val fileUploadApi: FileUploadApi,
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(ioDispatcher) {
        try {
            val file = createTempFileFromUri(imageUri) ?: return@withContext Result.failure(
                Exception("Failed to create file from URI")
            )

            val mimeType = getMimeType(imageUri) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            when (val response = NetworkUtils.safeApiCall { fileUploadApi.uploadImage(body) }) {
                is ApiResponse.Success -> {
                    Result.success(response.data.imageUrl)
                }

                is ApiResponse.Error -> {
                    Result.failure(Exception("Failed to upload image: ${response.message}"))
                }

                ApiResponse.Loading -> {
                    Result.failure(Exception("Request is still loading"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createTempFileFromUri(uri: Uri): File? = withContext(ioDispatcher) {
        try {
            val inputStream =
                context.contentResolver.openInputStream(uri) ?: return@withContext null
            val fileExtension = getMimeType(uri)?.substringAfter('/') ?: "jpg"
            val tempFile = File.createTempFile("upload_", ".$fileExtension", context.cacheDir)

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            return@withContext tempFile
        } catch (_: Exception) {
            return@withContext null
        }
    }

    private fun getMimeType(uri: Uri): String? {
        if (uri.scheme == "content") {
            return context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }
}