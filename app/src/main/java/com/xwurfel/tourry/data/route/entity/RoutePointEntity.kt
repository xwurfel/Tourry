package com.xwurfel.tourry.data.route.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xwurfel.tourry.data.tour.entity.TourEntity

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = TourEntity::class,
            parentColumns = ["id"],
            childColumns = ["tourId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tourId")
    ]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tourId: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String,
    val order: Int,
    val durationMinutes: Int? = null,
    val arrivalInstructions: String? = null,
    val imageUriString: String? = null
)