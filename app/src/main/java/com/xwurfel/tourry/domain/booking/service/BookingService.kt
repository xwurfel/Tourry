package com.xwurfel.tourry.domain.booking.service

import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingService @Inject constructor(
    private val bookingRepository: BookingRepository, private val tourRepository: TourRepository
) {
    suspend fun createBooking(
        tourId: Long, userId: Long, numberOfParticipants: Int, notes: String? = null
    ): Result<Long> {
        return try {
            if (numberOfParticipants <= 0) {
                return Result.failure(IllegalArgumentException("Number of participants must be greater than zero"))
            }

            val tour = tourRepository.getTourById(tourId).first() ?: return Result.failure(
                IllegalArgumentException("Tour not found")
            )

            if (tour.startDateTime.isBefore(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Cannot book a tour that has already started"))
            }

            val totalParticipants = bookingRepository.getTotalParticipantsForTour(tourId) ?: 0
            val remainingCapacity = tour.capacity - totalParticipants

            if (numberOfParticipants > remainingCapacity) {
                return Result.failure(
                    IllegalArgumentException(
                        "Not enough capacity. Remaining: $remainingCapacity, Requested: $numberOfParticipants"
                    )
                )
            }

            val totalPrice = numberOfParticipants * tour.price

            val booking = Booking(
                tourId = tourId,
                userId = userId,
                numberOfParticipants = numberOfParticipants,
                totalPrice = totalPrice,
                status = BookingStatus.PENDING,
                notes = notes,
                paymentProcessed = false
            )

            val bookingId = bookingRepository.createBooking(booking)
            Result.success(bookingId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmBooking(bookingId: Long): Result<Booking> {
        return try {
            val booking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    IllegalArgumentException("Booking not found")
                )

            if (booking.status != BookingStatus.PENDING) {
                return Result.failure(IllegalArgumentException("Booking is not in PENDING status"))
            }

            bookingRepository.updateBookingStatus(bookingId, BookingStatus.CONFIRMED)

            val updatedBooking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    Exception("Failed to update booking")
                )

            Result.success(updatedBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: Long): Result<Booking> {
        return try {
            val booking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    IllegalArgumentException("Booking not found")
                )

            if (booking.status == BookingStatus.CANCELLED) {
                return Result.failure(IllegalArgumentException("Booking is already cancelled"))
            }

            if (booking.status == BookingStatus.COMPLETED) {
                return Result.failure(IllegalArgumentException("Cannot cancel a completed booking"))
            }

            // Check if the tour has already started
            val tour = tourRepository.getTourById(booking.tourId).first() ?: return Result.failure(
                IllegalArgumentException("Tour not found")
            )

            if (tour.startDateTime.isBefore(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Cannot cancel a booking for a tour that has already started"))
            }

            bookingRepository.updateBookingStatus(bookingId, BookingStatus.CANCELLED)

            val updatedBooking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    Exception("Failed to update booking")
                )

            Result.success(updatedBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markBookingAsCompleted(bookingId: Long): Result<Booking> {
        return try {
            val booking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    IllegalArgumentException("Booking not found")
                )

            if (booking.status != BookingStatus.CONFIRMED) {
                return Result.failure(IllegalArgumentException("Only confirmed bookings can be marked as completed"))
            }

            // Check if the tour has already ended
            val tour = tourRepository.getTourById(booking.tourId).first() ?: return Result.failure(
                IllegalArgumentException("Tour not found")
            )

            if (tour.endDateTime.isAfter(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Cannot mark a booking as completed for a tour that hasn't ended yet"))
            }

            bookingRepository.updateBookingStatus(bookingId, BookingStatus.COMPLETED)

            val updatedBooking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    Exception("Failed to update booking")
                )

            Result.success(updatedBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processPayment(bookingId: Long): Result<Booking> {
        return try {
            val booking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    IllegalArgumentException("Booking not found")
                )

            if (booking.paymentProcessed) {
                return Result.failure(IllegalArgumentException("Payment already processed"))
            }

            // In a real app, this would integrate with a payment gateway
            // For now, we'll just mark it as processed
            bookingRepository.updatePaymentStatus(bookingId, true)

            val updatedBooking =
                bookingRepository.getBookingById(bookingId).first() ?: return Result.failure(
                    Exception("Failed to update booking")
                )

            Result.success(updatedBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllBookings(): Flow<List<Booking>> {
        return bookingRepository.getAllBookings()
    }

    fun getBookingById(bookingId: Long): Flow<Booking?> {
        return bookingRepository.getBookingById(bookingId)
    }

    fun getBookingsByUser(userId: Long): Flow<List<Booking>> {
        return bookingRepository.getBookingsByUser(userId)
    }

    fun getBookingsByTour(tourId: Long): Flow<List<Booking>> {
        return bookingRepository.getBookingsByTour(tourId)
    }

    fun getBookingsByTourAndStatus(tourId: Long, status: BookingStatus): Flow<List<Booking>> {
        return bookingRepository.getBookingsByTourAndStatus(tourId, status)
    }

    fun getBookingsByUserAndStatus(userId: Long, status: BookingStatus): Flow<List<Booking>> {
        return bookingRepository.getBookingsByUserAndStatus(userId, status)
    }
}