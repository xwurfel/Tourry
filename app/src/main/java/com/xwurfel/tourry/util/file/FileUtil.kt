package com.xwurfel.tourry.util.file

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object FileUtil {
    fun createTempFileFromUri(context: Context, uri: Uri, prefix: String): File? {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""

            val file = File(context.cacheDir, "$prefix-${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun compressImageIfNeeded(file: File, maxSizeInBytes: Long = 1024 * 1024): File {
        // TODO: use image compressing library
        // This would be a more complex implementation that would compress the image if it's too large
        // For simplicity, we'll just return the original file
        return file
    }
}