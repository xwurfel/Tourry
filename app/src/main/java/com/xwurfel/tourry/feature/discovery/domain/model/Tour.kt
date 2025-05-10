package com.xwurfel.tourry.feature.discovery.domain.model

import com.google.android.gms.maps.model.LatLng
import java.time.Duration

enum class TourDifficulty {
    EASY, MODERATE, CHALLENGING
}

enum class TourCategory {
    HISTORICAL, CULTURAL, NATURE, CULINARY, ADVENTURE, URBAN, EDUCATIONAL
}

data class TourPreview(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val location: String,
    val distance: Double?, // km
    val duration: Duration,
    val difficulty: TourDifficulty,
    val category: TourCategory,
    val rating: Float,
    val reviewCount: Int,
    val price: Double?,
    val isBookmarked: Boolean
)

data class TourDetails(
    val id: String,
    val title: String,
    val description: String,
    val creatorName: String,
    val creatorId: String,
    val thumbnailUrl: String?,
    val imageUrls: List<String>,
    val location: String,
    val startPoint: LatLng?,
    val distance: Double?, // km
    val duration: Duration,
    val difficulty: TourDifficulty,
    val category: TourCategory,
    val rating: Float,
    val reviewCount: Int,
    val price: Double?,
    val isBookmarked: Boolean,
    val waypointCount: Int,
    val highlights: List<String>
)

data class TourFilter(
    val query: String? = null,
    val location: String? = null,
    val categories: List<TourCategory> = emptyList(),
    val difficulties: List<TourDifficulty> = emptyList(),
    val minDuration: Duration? = null,
    val maxDuration: Duration? = null,
    val minRating: Float? = null,
    val maxPrice: Double? = null
)