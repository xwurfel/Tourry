package com.xwurfel.tourry.data.booking.repository

import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.mapper.toDomain
import com.xwurfel.tourry.data.booking.mapper.toEntity
import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val bookingDao: BookingDao
) : BookingRepository {

    override suspend fun createBooking(booking: Booking): Long {
        return bookingDao.insertBooking(booking.toEntity())
    }

    override fun getAllBookings(): Flow<List<Booking>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBookingById(id: Long): Flow<Booking?> {
        return bookingDao.getBookingById(id).map { it?.toDomain() }
    }

    override fun getBookingsByUser(userId: Long): Flow<List<Booking>> {
        return bookingDao.getBookingsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBookingsByTour(tourId: Long): Flow<List<Booking>> {
        return bookingDao.getBookingsByTour(tourId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBookingsByTourAndStatus(
        tourId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> {
        return bookingDao.getBookingsByTourAndStatus(tourId, status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBookingsByUserAndStatus(
        userId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> {
        return bookingDao.getBookingsByUserAndStatus(userId, status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateBookingStatus(id: Long, status: BookingStatus) {
        bookingDao.updateBookingStatus(id, status)
    }

    override suspend fun updatePaymentStatus(id: Long, processed: Boolean) {
        bookingDao.updatePaymentStatus(id, processed)
    }

    override suspend fun getActiveBookingsCountForTour(tourId: Long): Int {
        return bookingDao.getActiveBookingsCountForTour(tourId)
    }

    override suspend fun getTotalParticipantsForTour(tourId: Long): Int? {
        return bookingDao.getTotalParticipantsForTour(tourId)
    }

    override suspend fun deleteBooking(id: Long) {
        bookingDao.deleteBooking(id)
    }
}