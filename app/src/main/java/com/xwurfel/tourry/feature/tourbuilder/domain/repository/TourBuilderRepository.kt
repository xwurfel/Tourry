package com.xwurfel.tourry.feature.tourbuilder.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.TourDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Duration

interface TourBuilderRepository {
    suspend fun createTour(tourDraft: TourDraft): Result<TourDraft>
    suspend fun updateTour(tourDraft: TourDraft): Result<TourDraft>
    suspend fun getTourDraft(tourId: String): Result<TourDraft>
    suspend fun getMySavedTours(): Result<List<TourDraft>>
    suspend fun deleteTour(tourId: String): Result<Unit>

    suspend fun addWaypoint(tourId: String, waypoint: WaypointDraft): Result<WaypointDraft>
    suspend fun updateWaypoint(tourId: String, waypoint: WaypointDraft): Result<WaypointDraft>
    suspend fun deleteWaypoint(tourId: String, waypointId: String): Result<Unit>
    suspend fun reorderWaypoints(
        tourId: String,
        waypointIds: List<String>
    ): Result<List<WaypointDraft>>

    suspend fun addContent(
        tourId: String,
        waypointId: String,
        content: ContentDraft
    ): Result<ContentDraft>

    suspend fun updateContent(
        tourId: String,
        waypointId: String,
        content: ContentDraft
    ): Result<ContentDraft>

    suspend fun deleteContent(tourId: String, waypointId: String, contentId: String): Result<Unit>

    suspend fun uploadImage(file: File): Result<String>
    suspend fun uploadAudio(file: File): Result<String>

    suspend fun calculateRoute(waypoints: List<LatLng>): Result<RouteInfo>

    fun getDraftInProgress(): Flow<TourDraft?>
    suspend fun saveDraftLocally(tourDraft: TourDraft)
    suspend fun clearLocalDraft()
}

data class RouteInfo(
    val distance: Double,
    val duration: Duration,
    val path: List<LatLng>
)