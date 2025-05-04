package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.BookingCreateDto
import com.xwurfel.tourry.data.network.dto.BookingResponseDto
import com.xwurfel.tourry.data.network.dto.UpdateBookingStatusDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BookingApi {
    @POST("bookings")
    suspend fun createBooking(@Body bookingCreateDto: BookingCreateDto): Response<BookingResponseDto>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: Long): Response<BookingResponseDto>

    @GET("bookings")
    suspend fun getAllBookings(): Response<List<BookingResponseDto>>

    @GET("bookings/me")
    suspend fun getCurrentUserBookings(): Response<List<BookingResponseDto>>

    @GET("bookings/tour/{tourId}")
    suspend fun getBookingsForTour(@Path("tourId") tourId: Long): Response<List<BookingResponseDto>>

    @PUT("bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") id: Long,
        @Body statusDto: UpdateBookingStatusDto
    ): Response<BookingResponseDto>

    @POST("bookings/{id}/payment")
    suspend fun processPayment(@Path("id") id: Long): Response<BookingResponseDto>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: Long): Response<Unit>
}