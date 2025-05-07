package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.RoutePointCreateDto
import com.xwurfel.tourry.data.network.dto.RoutePointDto
import com.xwurfel.tourry.data.network.dto.TourRouteUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RouteApi {
    @GET("routes/tour/{tourId}")
    suspend fun getRoutePointsForTour(@Path("tourId") tourId: Long): Response<List<RoutePointDto>>

    @GET("routes/point/{id}")
    suspend fun getRoutePointById(@Path("id") id: Long): Response<RoutePointDto>

    @POST("routes/point")
    suspend fun createRoutePoint(@Body routePoint: RoutePointCreateDto): Response<RoutePointDto>

    @PUT("routes/point/{id}")
    suspend fun updateRoutePoint(
        @Path("id") id: Long,
        @Body routePoint: RoutePointCreateDto
    ): Response<RoutePointDto>

    @PUT("routes/tour")
    suspend fun updateTourRoutePoints(@Body tourRoute: TourRouteUpdateDto): Response<List<RoutePointDto>>

    @DELETE("routes/point/{id}")
    suspend fun deleteRoutePoint(@Path("id") id: Long): Response<Unit>
}