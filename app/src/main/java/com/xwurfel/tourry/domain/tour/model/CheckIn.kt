package com.xwurfel.tourry.domain.tour.model

import java.time.LocalDateTime

data class CheckIn(
    val id: Long = 0,
    val userId: Long,
    val tourId: Long,
    val routePointId: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val note: String? = null,
    val imageUri: String? = null
)