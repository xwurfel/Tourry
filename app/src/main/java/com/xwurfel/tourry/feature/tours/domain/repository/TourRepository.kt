package com.xwurfel.tourry.feature.tours.domain.repository

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.model.LiveParticipant
import com.xwurfel.tourry.feature.tours.domain.model.LiveTour
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourParticipation
import kotlinx.coroutines.flow.Flow

interface TourRepository {

    // Tour discovery and browsing
    fun observeAvailableTours(): Flow<List<Tour>>
    fun observeToursByAuthor(authorId: String): Flow<List<Tour>>
    suspend fun getTourById(tourId: String): DomainResult<Tour>
    suspend fun searchTours(
        query: String,
        themes: List<String> = emptyList(),
        maxPrice: Double? = null,
        maxDistance: Float? = null,
        userLocation: Pair<Double, Double>? = null
    ): DomainResult<List<Tour>>

    // Tour management
    suspend fun createTour(request: CreateTourRequest): DomainResult<String>
    suspend fun updateTour(tourId: String, request: CreateTourRequest): DomainResult<Unit>
    suspend fun deleteTour(tourId: String): DomainResult<Unit>
    suspend fun setTourLiveStatus(tourId: String, isLive: Boolean): DomainResult<Unit>

    // Participation management
    suspend fun joinTour(tourId: String): DomainResult<Unit>
    suspend fun leaveTour(tourId: String): DomainResult<Unit>
    fun observeUserParticipations(userId: String): Flow<List<TourParticipation>>
    fun observeTourParticipations(tourId: String): Flow<List<TourParticipation>>

    // Reviews and ratings
    suspend fun submitReview(
        tourId: String,
        rating: Int,
        review: String,
        completionPercentage: Float
    ): DomainResult<Unit>

    // Real-time features
    suspend fun updateUserLocation(
        tourId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        currentStopId: String? = null
    ): DomainResult<Unit>

    fun observeLiveParticipants(tourId: String): Flow<List<LiveParticipant>>

    suspend fun getLiveTour(tourId: String): DomainResult<LiveTour>
    suspend fun startTourSession(tourId: String, userId: String): DomainResult<String>
    suspend fun recordStopVisit(
        sessionId: String,
        stopId: String,
        timestamp: Long,
        userLocation: Pair<Double, Double>
    ): DomainResult<Unit>

    suspend fun completeTourSession(
        sessionId: String,
        completionPercentage: Float,
        totalDuration: Long
    ): DomainResult<Unit>

    /**
     * Manually starts a tour, marking it as live
     */
    suspend fun startTour(tourId: String, userId: String): DomainResult<Unit>

    /**
     * Completes a tour for a specific user
     */
    suspend fun completeTour(tourId: String, userId: String): DomainResult<Unit>
}
