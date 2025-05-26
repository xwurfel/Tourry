package com.xwurfel.tourry.ui.main

// Base route constants - these are the actual route strings used in the navigation graph
const val exploreRoute = "explore"
const val myToursRoute = "my_tours"
const val profileRoute = "profile"
const val authRoute = "auth"

// Routes with arguments - these are the full route patterns used in composable()
const val tourDetailRoute = "tour_detail"
const val tourDetailRouteWithArgs = "$tourDetailRoute/{tourId}"

const val tourCreationRoute = "tour_creation"
const val tourCreationRouteWithArgs = "$tourCreationRoute?tourId={tourId}"

const val liveTourRoute = "live_tour"
const val liveTourRouteWithArgs = "$liveTourRoute/{tourId}"

const val tourSummaryRoute = "tour_summary"
const val tourSummaryRouteWithArgs = "$tourSummaryRoute/{tourId}"

object TourryNavigation {
    fun createTourDetailRoute(tourId: String) = "$tourDetailRoute/$tourId"
    fun createLiveTourRoute(tourId: String) = "$liveTourRoute/$tourId"
    fun createTourSummaryRoute(tourId: String) = "$tourSummaryRoute/$tourId"
    fun createTourCreationRoute(tourId: String? = null) =
        if (tourId != null) "$tourCreationRoute?tourId=$tourId" else tourCreationRoute
}