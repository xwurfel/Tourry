package com.xwurfel.tourry.data.checkin.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xwurfel.tourry.data.route.entity.RoutePointEntity
import com.xwurfel.tourry.data.tour.entity.TourEntity
import com.xwurfel.tourry.data.user.entity.UserEntity
import java.time.LocalDateTime

@Entity(
    tableName = "check_ins",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TourEntity::class,
            parentColumns = ["id"],
            childColumns = ["tourId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutePointEntity::class,
            parentColumns = ["id"],
            childColumns = ["routePointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("tourId"),
        Index("routePointId")
    ]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val tourId: Long,
    val routePointId: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val note: String? = null,
    val imageUriString: String? = null
)