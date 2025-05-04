package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.CheckInCreateDto
import com.xwurfel.tourry.data.network.dto.CheckInResponseDto
import com.xwurfel.tourry.data.network.dto.RoutePointWithStatusDto
import com.xwurfel.tourry.data.network.dto.TourProgressDto
import com.xwurfel.tourry.data.route.dto.RoutePointDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CheckInApi {
    @POST("checkins")
    suspend fun createCheckIn(@Body checkInCreateDto: CheckInCreateDto): Response<CheckInResponseDto>

    @GET("checkins/tour/{tourId}")
    suspend fun getCheckInsForTour(@Path("tourId") tourId: Long): Response<List<CheckInResponseDto>>

    @GET("checkins/tour/{tourId}/count")
    suspend fun getCheckInCountForTour(@Path("tourId") tourId: Long): Response<Map<String, Int>>

    @GET("checkins/tour/{tourId}/route-points")
    suspend fun getRoutePointsWithCheckInStatus(@Path("tourId") tourId: Long): Response<List<RoutePointWithStatusDto>>

    @GET("checkins/tour/{tourId}/next-point")
    suspend fun getNextRoutePointToVisit(@Path("tourId") tourId: Long): Response<RoutePointDto>

    @GET("checkins/tour/{tourId}/progress")
    suspend fun getTourProgress(@Path("tourId") tourId: Long): Response<TourProgressDto>
}