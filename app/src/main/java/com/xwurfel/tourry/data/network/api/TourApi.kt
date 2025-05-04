package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.TourCreateDto
import com.xwurfel.tourry.data.network.dto.TourMinDto
import com.xwurfel.tourry.data.network.dto.TourResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TourApi {
    @POST("tours")
    suspend fun createTour(@Body tourCreateDto: TourCreateDto): Response<TourResponseDto>

    @GET("tours/upcoming")
    suspend fun getUpcomingTours(): Response<List<TourMinDto>>

    @GET("tours")
    suspend fun getAllTours(): Response<List<TourMinDto>>

    @GET("tours/{id}")
    suspend fun getTourById(@Path("id") id: Long): Response<TourResponseDto>

    @GET("tours/search")
    suspend fun searchTours(@Query("query") query: String): Response<List<TourMinDto>>

    @GET("tours/category/{categoryId}")
    suspend fun getToursByCategory(@Path("categoryId") categoryId: Long): Response<List<TourMinDto>>

    @GET("tours/organizer/{organizerId}")
    suspend fun getToursByOrganizer(@Path("organizerId") organizerId: Long): Response<List<TourMinDto>>

    @GET("tours/{id}/remaining-capacity")
    suspend fun getRemainingCapacity(@Path("id") id: Long): Response<Map<String, Int>>

    @PUT("tours/{id}")
    suspend fun updateTour(
        @Path("id") id: Long,
        @Body tourCreateDto: TourCreateDto
    ): Response<TourResponseDto>

    @DELETE("tours/{id}")
    suspend fun deleteTour(@Path("id") id: Long): Response<Unit>
}