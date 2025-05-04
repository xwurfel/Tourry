package com.xwurfel.tourry.data.network.dto

import com.xwurfel.tourry.data.route.dto.RoutePointDto
import java.time.LocalDateTime

data class CheckInCreateDto(
    val tourId: Long,
    val routePointId: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String? = null,
    val imageUrl: String? = null,
    val forceCheckIn: Boolean = false
)

data class CheckInResponseDto(
    val id: Long,
    val user: UserMinDto,
    val tour: TourMinDto,
    val routePoint: RoutePointDto,
    val timestamp: LocalDateTime,
    val note: String?,
    val imageUrl: String?
)

data class RoutePointWithStatusDto(
    val routePoint: RoutePointDto,
    val isCheckedIn: Boolean
)

data class TourProgressDto(
    val tourId: Long,
    val tourTitle: String,
    val totalPoints: Int,
    val checkedInPoints: Int,
    val progress: Double,
    val nextPoint: RoutePointDto?
)