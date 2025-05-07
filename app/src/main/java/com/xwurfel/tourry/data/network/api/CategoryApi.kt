package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.CategoryCreateDto
import com.xwurfel.tourry.data.network.dto.CategoryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CategoryApi {
    @GET("categories")
    suspend fun getAllCategories(): Response<List<CategoryResponseDto>>

    @GET("categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): Response<CategoryResponseDto>

    @GET("categories/search")
    suspend fun searchCategories(@Query("query") query: String): Response<List<CategoryResponseDto>>

    @POST("categories")
    suspend fun createCategory(@Body categoryDto: CategoryCreateDto): Response<CategoryResponseDto>

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body categoryDto: CategoryCreateDto
    ): Response<CategoryResponseDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>
}