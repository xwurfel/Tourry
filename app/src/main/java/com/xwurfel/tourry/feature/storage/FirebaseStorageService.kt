package com.xwurfel.tourry.feature.storage

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


// TODO: USE THIS
@Singleton
class FirebaseStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TOUR_COVERS_PATH = "tour_covers"
        private const val STOP_IMAGES_PATH = "stop_images"
        private const val STOP_AUDIO_PATH = "stop_audio"
        private const val STOP_VIDEOS_PATH = "stop_videos"
        private const val AVATARS_PATH = "avatars"
        private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_AUDIO_SIZE = 20 * 1024 * 1024L // 20MB
        private const val MAX_VIDEO_SIZE = 100 * 1024 * 1024L // 100MB
    }

    suspend fun uploadTourCover(uri: Uri, tourId: String): DomainResult<String> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        validateFileSize(uri, MAX_IMAGE_SIZE)

        val fileName = "${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference
            .child(TOUR_COVERS_PATH)
            .child(currentUser.uid)
            .child(tourId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("uploadedBy", currentUser.uid)
            .setCustomMetadata("tourId", tourId)
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        Timber.d("Tour cover uploaded: $downloadUrl")
        downloadUrl.toString()
    }

    suspend fun uploadStopImage(uri: Uri, tourId: String, stopId: String): DomainResult<String> =
        result {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            validateFileSize(uri, MAX_IMAGE_SIZE)

            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child(STOP_IMAGES_PATH)
                .child(currentUser.uid)
                .child(tourId)
                .child(stopId)
                .child(fileName)

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploadedBy", currentUser.uid)
                .setCustomMetadata("tourId", tourId)
                .setCustomMetadata("stopId", stopId)
                .build()

            val uploadTask = storageRef.putFile(uri, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            Timber.d("Stop image uploaded: $downloadUrl")
            downloadUrl.toString()
        }

    suspend fun uploadStopAudio(uri: Uri, tourId: String, stopId: String): DomainResult<String> =
        result {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            validateFileSize(uri, MAX_AUDIO_SIZE)

            val fileName = "${UUID.randomUUID()}.mp3"
            val storageRef = storage.reference
                .child(STOP_AUDIO_PATH)
                .child(currentUser.uid)
                .child(tourId)
                .child(stopId)
                .child(fileName)

            val metadata = StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .setCustomMetadata("uploadedBy", currentUser.uid)
                .setCustomMetadata("tourId", tourId)
                .setCustomMetadata("stopId", stopId)
                .build()

            val uploadTask = storageRef.putFile(uri, metadata).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            Timber.d("Stop audio uploaded: $downloadUrl")
            downloadUrl.toString()
        }

    suspend fun uploadUserAvatar(uri: Uri): DomainResult<String> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        validateFileSize(uri, MAX_IMAGE_SIZE)

        val fileName = "${currentUser.uid}_avatar.jpg"
        val storageRef = storage.reference
            .child(AVATARS_PATH)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("uploadedBy", currentUser.uid)
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        Timber.d("Avatar uploaded: $downloadUrl")
        downloadUrl.toString()
    }

    suspend fun deleteFile(downloadUrl: String): DomainResult<Unit> = result {
        val storageRef = storage.getReferenceFromUrl(downloadUrl)
        storageRef.delete().await()
        Timber.d("File deleted: $downloadUrl")
    }

    suspend fun deleteTourFiles(tourId: String): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Delete tour cover
        val tourCoverRef = storage.reference
            .child(TOUR_COVERS_PATH)
            .child(currentUser.uid)
            .child(tourId)

        deleteAllFilesInPath(tourCoverRef)

        // Delete stop images
        val stopImagesRef = storage.reference
            .child(STOP_IMAGES_PATH)
            .child(currentUser.uid)
            .child(tourId)

        deleteAllFilesInPath(stopImagesRef)

        // Delete stop audio
        val stopAudioRef = storage.reference
            .child(STOP_AUDIO_PATH)
            .child(currentUser.uid)
            .child(tourId)

        deleteAllFilesInPath(stopAudioRef)

        Timber.d("All files deleted for tour: $tourId")
    }

    private suspend fun deleteAllFilesInPath(storageRef: com.google.firebase.storage.StorageReference) {
        try {
            val listResult = storageRef.listAll().await()
            listResult.items.forEach { fileRef ->
                fileRef.delete().await()
            }
            listResult.prefixes.forEach { folderRef ->
                deleteAllFilesInPath(folderRef)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error deleting files in path: ${storageRef.path}")
        }
    }

    private fun validateFileSize(uri: Uri, maxSize: Long) {
        val fileSize = getFileSize(uri)
        if (fileSize > maxSize) {
            throw Exception("File size exceeds limit. Max size: ${maxSize / (1024 * 1024)}MB")
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.statSize
            } ?: 0L
        } catch (e: Exception) {
            Timber.w(e, "Error getting file size")
            0L
        }
    }

    fun getUploadProgress(uploadTaskSnapshot: com.google.firebase.storage.UploadTask.TaskSnapshot): Double {
        return (100.0 * uploadTaskSnapshot.bytesTransferred) / uploadTaskSnapshot.totalByteCount
    }
}