package com.xwurfel.tourry.feature.tourbuilder.api

import com.xwurfel.tourry.feature.tourbuilder.api.model.ContentDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.LatLngDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.MediaUploadResponseDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.RouteInfoDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.TourDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.WaypointDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.WaypointOrderDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface TourBuilderApi {
    @POST("tours/drafts")
    suspend fun createTour(@Body tourDraft: TourDraftDto): Response<TourDraftDto>

    @PUT("tours/drafts/{tourId}")
    suspend fun updateTour(
        @Path("tourId") tourId: String,
        @Body tourDraft: TourDraftDto
    ): Response<TourDraftDto>

    @GET("tours/drafts/{tourId}")
    suspend fun getTourDraft(@Path("tourId") tourId: String): Response<TourDraftDto>

    @GET("tours/drafts")
    suspend fun getMySavedTours(): Response<List<TourDraftDto>>

    @DELETE("tours/drafts/{tourId}")
    suspend fun deleteTour(@Path("tourId") tourId: String): Response<Unit>

    @POST("tours/drafts/{tourId}/waypoints")
    suspend fun addWaypoint(
        @Path("tourId") tourId: String,
        @Body waypoint: WaypointDraftDto
    ): Response<WaypointDraftDto>

    @PUT("tours/drafts/{tourId}/waypoints/{waypointId}")
    suspend fun updateWaypoint(
        @Path("tourId") tourId: String,
        @Path("waypointId") waypointId: String,
        @Body waypoint: WaypointDraftDto
    ): Response<WaypointDraftDto>

    @DELETE("tours/drafts/{tourId}/waypoints/{waypointId}")
    suspend fun deleteWaypoint(
        @Path("tourId") tourId: String,
        @Path("waypointId") waypointId: String
    ): Response<Unit>

    @PUT("tours/drafts/{tourId}/waypoints/order")
    suspend fun reorderWaypoints(
        @Path("tourId") tourId: String,
        @Body orderRequest: WaypointOrderDto
    ): Response<List<WaypointDraftDto>>

    @POST("tours/drafts/{tourId}/waypoints/{waypointId}/contents")
    suspend fun addContent(
        @Path("tourId") tourId: String,
        @Path("waypointId") waypointId: String,
        @Body content: ContentDraftDto
    ): Response<ContentDraftDto>

    @PUT("tours/drafts/{tourId}/waypoints/{waypointId}/contents/{contentId}")
    suspend fun updateContent(
        @Path("tourId") tourId: String,
        @Path("waypointId") waypointId: String,
        @Path("contentId") contentId: String,
        @Body content: ContentDraftDto
    ): Response<ContentDraftDto>

    @DELETE("tours/drafts/{tourId}/waypoints/{waypointId}/contents/{contentId}")
    suspend fun deleteContent(
        @Path("tourId") tourId: String,
        @Path("waypointId") waypointId: String,
        @Path("contentId") contentId: String
    ): Response<Unit>

    @Multipart
    @POST("media/images")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<MediaUploadResponseDto>

    @Multipart
    @POST("media/audio")
    suspend fun uploadAudio(@Part file: MultipartBody.Part): Response<MediaUploadResponseDto>

    @POST("tours/route")
    suspend fun calculateRoute(@Body waypoints: List<LatLngDto>): Response<RouteInfoDto>
}