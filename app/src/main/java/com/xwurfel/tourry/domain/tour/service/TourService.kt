package com.xwurfel.tourry.domain.tour.service

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourService @Inject constructor(
    private val tourRepository: TourRepository,
    private val bookingRepository: BookingRepository
) {
    suspend fun createTour(
        title: String,
        description: String,
        meetingPoint: LatLng,
        meetingPointAddress: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        price: Double,
        capacity: Int,
        categoryId: Long,
        organizerId: Long
    ): Result<Long> {
        return try {
            if (title.isBlank()) {
                return Result.failure(IllegalArgumentException("Title cannot be empty"))
            }

            if (description.isBlank()) {
                return Result.failure(IllegalArgumentException("Description cannot be empty"))
            }

            if (meetingPointAddress.isBlank()) {
                return Result.failure(IllegalArgumentException("Meeting point address cannot be empty"))
            }

            if (startDateTime.isBefore(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Start date cannot be in the past"))
            }

            if (endDateTime.isBefore(startDateTime)) {
                return Result.failure(IllegalArgumentException("End date cannot be before start date"))
            }

            if (price < 0) {
                return Result.failure(IllegalArgumentException("Price cannot be negative"))
            }

            if (capacity <= 0) {
                return Result.failure(IllegalArgumentException("Capacity must be greater than zero"))
            }

            val tour = Tour(
                title = title,
                description = description,
                imageUri = null,
                meetingPoint = meetingPoint,
                meetingPointAddress = meetingPointAddress,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                price = price,
                capacity = capacity,
                categoryId = categoryId,
                organizerId = organizerId
            )

            val tourId = tourRepository.saveTour(tour)
            Result.success(tourId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTour(
        tourId: Long,
        title: String? = null,
        description: String? = null,
        imageUri: Uri? = null,
        meetingPoint: LatLng? = null,
        meetingPointAddress: String? = null,
        startDateTime: LocalDateTime? = null,
        endDateTime: LocalDateTime? = null,
        price: Double? = null,
        capacity: Int? = null,
        categoryId: Long? = null
    ): Result<Tour> {
        return try {
            val existingTour = tourRepository.getTourById(tourId).first()
                ?: return Result.failure(IllegalArgumentException("Tour not found"))

            // Validate fields if they're being updated
            if (title != null && title.isBlank()) {
                return Result.failure(IllegalArgumentException("Title cannot be empty"))
            }

            if (description != null && description.isBlank()) {
                return Result.failure(IllegalArgumentException("Description cannot be empty"))
            }

            if (meetingPointAddress != null && meetingPointAddress.isBlank()) {
                return Result.failure(IllegalArgumentException("Meeting point address cannot be empty"))
            }

            val newStartDateTime = startDateTime ?: existingTour.startDateTime
            val newEndDateTime = endDateTime ?: existingTour.endDateTime

            if (startDateTime != null && startDateTime.isBefore(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Start date cannot be in the past"))
            }

            if (newEndDateTime.isBefore(newStartDateTime)) {
                return Result.failure(IllegalArgumentException("End date cannot be before start date"))
            }

            if (price != null && price < 0) {
                return Result.failure(IllegalArgumentException("Price cannot be negative"))
            }

            if (capacity != null && capacity <= 0) {
                return Result.failure(IllegalArgumentException("Capacity must be greater than zero"))
            }

            // Check if reducing capacity would cause overbooking
            if (capacity != null && capacity < existingTour.capacity) {
                val currentParticipants = bookingRepository.getTotalParticipantsForTour(tourId) ?: 0
                if (capacity < currentParticipants) {
                    return Result.failure(IllegalArgumentException("Cannot reduce capacity below current bookings ($currentParticipants participants)"))
                }
            }

            val updatedTour = existingTour.copy(
                title = title ?: existingTour.title,
                description = description ?: existingTour.description,
                imageUri = imageUri ?: existingTour.imageUri,
                meetingPoint = meetingPoint ?: existingTour.meetingPoint,
                meetingPointAddress = meetingPointAddress ?: existingTour.meetingPointAddress,
                startDateTime = newStartDateTime,
                endDateTime = newEndDateTime,
                price = price ?: existingTour.price,
                capacity = capacity ?: existingTour.capacity,
                categoryId = categoryId ?: existingTour.categoryId,
                updatedAt = LocalDateTime.now()
            )

            tourRepository.saveTour(updatedTour)

            val refreshedTour = tourRepository.getTourById(tourId).first()
                ?: return Result.failure(Exception("Failed to update tour"))

            Result.success(refreshedTour)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTour(tourId: Long): Result<Unit> {
        return try {
            if (doesTourExist(tourId).not()) {
                return Result.failure(IllegalArgumentException("Tour not found"))
            }

            val bookings = bookingRepository.getBookingsByTour(tourId).first()
            if (bookings.isNotEmpty()) {
                val activeBookings = bookings.filter { it.status != BookingStatus.CANCELLED }
                if (activeBookings.isNotEmpty()) {
                    return Result.failure(IllegalArgumentException("Cannot delete tour with active bookings"))
                }
            }

            tourRepository.deleteTour(tourId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllTours(): Flow<List<Tour>> {
        return tourRepository.getAllTours()
    }

    fun getTourById(tourId: Long): Flow<Tour?> {
        return tourRepository.getTourById(tourId)
    }

    fun getToursByOrganizer(organizerId: Long): Flow<List<Tour>> {
        return tourRepository.getToursByOrganizer(organizerId)
    }

    fun getToursByCategory(categoryId: Long): Flow<List<Tour>> {
        return tourRepository.getToursByCategory(categoryId)
    }

    fun getUpcomingTours(): Flow<List<Tour>> {
        return tourRepository.getUpcomingTours()
    }

    fun searchTours(query: String): Flow<List<Tour>> {
        return tourRepository.searchTours(query)
    }

    suspend fun getRemainingCapacity(tourId: Long): Result<Int> {
        return try {
            val tour = tourRepository.getTourById(tourId).first()
                ?: return Result.failure(IllegalArgumentException("Tour not found"))

            val totalParticipants = bookingRepository.getTotalParticipantsForTour(tourId) ?: 0
            val remainingCapacity = tour.capacity - totalParticipants

            Result.success(remainingCapacity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isTourFull(tourId: Long): Result<Boolean> {
        return try {
            val remainingCapacity = getRemainingCapacity(tourId).getOrThrow()
            Result.success(remainingCapacity <= 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun doesTourExist(tourId: Long): Boolean {
        return tourRepository.getTourById(tourId).first() != null
    }
}
