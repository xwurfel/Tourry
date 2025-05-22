package com.xwurfel.tourry.ui.navigation


// Route constants
const val exploreRoute = "explore"
const val myToursRoute = "my_tours"
const val profileRoute = "profile"
const val tourDetailRoute = "tour_detail"
const val tourCreationRoute = "tour_creation"
const val liveTourRoute = "live_tour"
const val tourSummaryRoute = "tour_summary"
const val authRoute = "auth"

// Route with arguments
const val tourDetailRouteWithArgs = "$tourDetailRoute/{tourId}"
const val liveTourRouteWithArgs = "$liveTourRoute/{tourId}"
const val tourSummaryRouteWithArgs = "$tourSummaryRoute/{tourId}"
const val tourCreationRouteWithArgs = "$tourCreationRoute?tourId={tourId}"