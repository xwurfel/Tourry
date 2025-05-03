package com.xwurfel.tourry.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destinations {
    @Serializable
    data object Home : Destinations

    @Serializable
    data class PoiSettings(val latitude: Double, val longitude: Double) : Destinations

    @Serializable
    data class PoiDetails(val poiId: Long) : Destinations

    @Serializable
    data object TourList : Destinations

    @Serializable
    data class TourDetails(val tourId: Long) : Destinations

    @Serializable
    data class EditTour(val tourId: Long? = null) : Destinations

    @Serializable
    data class BookTour(val tourId: Long) : Destinations

    @Serializable
    data object MyBookings : Destinations

    @Serializable
    data object Profile : Destinations

    @Serializable
    data object Login : Destinations

    @Serializable
    data object Register : Destinations
}