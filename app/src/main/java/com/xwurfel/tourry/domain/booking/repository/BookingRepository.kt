package com.xwurfel.tourry.domain.booking.repository

import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    suspend fun createBooking(booking: Booking): Long

    fun getAllBookings(): Flow<List<Booking>>

    fun getBookingById(id: Long): Flow<Booking?>

    fun getBookingsByUser(userId: Long): Flow<List<Booking>>

    fun getBookingsByTour(tourId: Long): Flow<List<Booking>>

    fun getBookingsByTourAndStatus(tourId: Long, status: BookingStatus): Flow<List<Booking>>

    fun getBookingsByUserAndStatus(userId: Long, status: BookingStatus): Flow<List<Booking>>

    suspend fun updateBookingStatus(id: Long, status: BookingStatus)

    suspend fun updatePaymentStatus(id: Long, processed: Boolean)

    suspend fun getActiveBookingsCountForTour(tourId: Long): Int

    suspend fun getTotalParticipantsForTour(tourId: Long): Int?

    suspend fun deleteBooking(id: Long)
}