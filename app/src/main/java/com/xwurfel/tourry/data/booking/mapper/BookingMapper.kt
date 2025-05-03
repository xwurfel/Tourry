package com.xwurfel.tourry.data.booking.mapper

import com.xwurfel.tourry.data.booking.entity.BookingEntity
import com.xwurfel.tourry.domain.booking.model.Booking

fun BookingEntity.toDomain(): Booking {
    return Booking(
        id = id,
        tourId = tourId,
        userId = userId,
        numberOfParticipants = numberOfParticipants,
        totalPrice = totalPrice,
        status = status,
        notes = notes,
        paymentProcessed = paymentProcessed,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Booking.toEntity(): BookingEntity {
    return BookingEntity(
        id = id,
        tourId = tourId,
        userId = userId,
        numberOfParticipants = numberOfParticipants,
        totalPrice = totalPrice,
        status = status,
        notes = notes,
        paymentProcessed = paymentProcessed,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}