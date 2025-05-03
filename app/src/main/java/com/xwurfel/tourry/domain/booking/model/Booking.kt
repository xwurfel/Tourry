package com.xwurfel.tourry.domain.booking.model

import java.time.LocalDateTime

data class Booking(
    val id: Long = 0,
    val tourId: Long,
    val userId: Long,
    val numberOfParticipants: Int,
    val totalPrice: Double,
    val status: BookingStatus,
    val notes: String?,
    val paymentProcessed: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)