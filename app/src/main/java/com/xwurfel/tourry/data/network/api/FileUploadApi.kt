package com.xwurfel.tourry.data.network.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FileUploadApi {
    @Multipart
    @POST("files/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<ImageUploadResponseDto>
}

data class ImageUploadResponseDto(
    val imageUrl: String
)