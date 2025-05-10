package com.xwurfel.tourry.feature.tourbuilder.domain.model

import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.feature.discovery.domain.model.TourCategory
import com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty
import java.time.Duration

data class TourDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val category: TourCategory = TourCategory.CULTURAL,
    val difficulty: TourDifficulty = TourDifficulty.MODERATE,
    val price: Double? = null,
    val isPublic: Boolean = false,
    val waypoints: List<WaypointDraft> = emptyList(),
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val highlights: List<String> = emptyList(),
    val estimatedDuration: Duration = Duration.ZERO
)

data class WaypointDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val position: LatLng,
    val order: Int,
    val durationMinutes: Int = 30,
    val geofenceRadius: Float = 50f,
    val contents: List<ContentDraft> = emptyList()
)

data class ContentDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val type: ContentType = ContentType.TEXT,
    val mediaUrl: String? = null,
    val order: Int
)

enum class ContentType {
    TEXT, IMAGE, AUDIO
}