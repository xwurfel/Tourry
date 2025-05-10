package com.xwurfel.tourry.feature.discovery.api

import com.xwurfel.tourry.feature.discovery.api.model.BookmarkRequestDto
import com.xwurfel.tourry.feature.discovery.api.model.TourDetailsDto
import com.xwurfel.tourry.feature.discovery.api.model.TourPreviewDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TourDiscoveryApi {
    @GET("tours")
    suspend fun searchTours(
        @Query("query") query: String?,
        @Query("location") location: String?,
        @Query("categories") categories: String?,
        @Query("difficulties") difficulties: String?,
        @Query("min_duration") minDurationMinutes: Int?,
        @Query("max_duration") maxDurationMinutes: Int?,
        @Query("min_rating") minRating: Float?,
        @Query("max_price") maxPrice: Double?
    ): Response<List<TourPreviewDto>>

    @GET("tours/{id}")
    suspend fun getTourDetails(
        @Path("id") tourId: String
    ): Response<TourDetailsDto>

    @GET("tours/featured")
    suspend fun getFeaturedTours(): Response<List<TourPreviewDto>>

    @GET("tours/popular")
    suspend fun getPopularTours(): Response<List<TourPreviewDto>>

    @GET("tours/nearby")
    suspend fun getNearbyTours(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radiusKm: Double
    ): Response<List<TourPreviewDto>>

    @GET("tours/bookmarked")
    suspend fun getBookmarkedTours(): Response<List<TourPreviewDto>>

    @POST("tours/bookmark")
    suspend fun bookmarkTour(
        @Body request: BookmarkRequestDto
    ): Response<Unit>

    @DELETE("tours/bookmark/{tourId}")
    suspend fun removeBookmark(
        @Path("tourId") tourId: String
    ): Response<Unit>
}