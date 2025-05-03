package com.xwurfel.tourry.data.booking.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xwurfel.tourry.data.tour.entity.TourEntity
import com.xwurfel.tourry.data.user.entity.UserEntity
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import java.time.LocalDateTime

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = TourEntity::class,
            parentColumns = ["id"],
            childColumns = ["tourId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index("tourId"),
        Index("userId")
    ]
)
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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