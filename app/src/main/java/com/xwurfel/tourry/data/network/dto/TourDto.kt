package com.xwurfel.tourry.data.network.dto

import java.time.LocalDateTime

data class TourCreateDto(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val meetingPointLatitude: Double,
    val meetingPointLongitude: Double,
    val meetingPointAddress: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val price: Double,
    val capacity: Int,
    val categoryId: Long,
    val organizerId: Long
)

data class TourResponseDto(
    val id: Long,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val meetingPointLatitude: Double,
    val meetingPointLongitude: Double,
    val meetingPointAddress: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val price: Double,
    val capacity: Int,
    val remainingCapacity: Int,
    val category: CategoryMinDto,
    val organizer: UserMinDto,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TourMinDto(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val meetingPointAddress: String,
    val startDateTime: LocalDateTime,
    val price: Double,
    val category: CategoryMinDto,
    val remainingCapacity: Int
)

data class CategoryMinDto(
    val id: Long,
    val name: String,
    val iconUrl: String?
)

data class UserMinDto(
    val id: Long,
    val name: String,
    val profileImageUrl: String?
)