package com.xwurfel.tourry.feature.discovery.domain.repository

import com.xwurfel.tourry.feature.discovery.domain.model.TourDetails
import com.xwurfel.tourry.feature.discovery.domain.model.TourFilter
import com.xwurfel.tourry.feature.discovery.domain.model.TourPreview
import kotlinx.coroutines.flow.Flow

interface TourDiscoveryRepository {
    suspend fun searchTours(filter: TourFilter): Result<List<TourPreview>>
    suspend fun getTourDetails(tourId: String): Result<TourDetails>
    suspend fun getFeaturedTours(): Result<List<TourPreview>>
    suspend fun getPopularTours(): Result<List<TourPreview>>
    suspend fun getNearbyTours(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0
    ): Result<List<TourPreview>>

    suspend fun getBookmarkedTours(): Result<List<TourPreview>>
    suspend fun bookmarkTour(tourId: String): Result<Unit>
    suspend fun removeBookmark(tourId: String): Result<Unit>
    fun observeBookmarkedTours(): Flow<List<TourPreview>>
}