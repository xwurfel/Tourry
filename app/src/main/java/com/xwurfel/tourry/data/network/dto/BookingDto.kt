package com.xwurfel.tourry.data.network.dto

import java.time.LocalDateTime

data class BookingCreateDto(
    val tourId: Long,
    val numberOfParticipants: Int,
    val notes: String? = null
)

data class BookingResponseDto(
    val id: Long,
    val tour: TourMinDto,
    val user: UserMinDto,
    val numberOfParticipants: Int,
    val totalPrice: Double,
    val status: String,  // Will be converted to enum
    val notes: String?,
    val paymentProcessed: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class UpdateBookingStatusDto(
    val status: String  // From enum BookingStatus
)

data class BookingMinDto(
    val id: Long,
    val tourId: Long,
    val tourTitle: String,
    val startDateTime: LocalDateTime,
    val numberOfParticipants: Int,
    val totalPrice: Double,
    val status: String  // Will be converted to enum
)