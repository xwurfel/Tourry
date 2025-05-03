package com.xwurfel.tourry.data.tour.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tours")
data class TourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imageUriString: String?,
    val meetingPointLatitude: Double,
    val meetingPointLongitude: Double,
    val meetingPointAddress: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val price: Double,
    val capacity: Int,
    val categoryId: Long,
    val organizerId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)