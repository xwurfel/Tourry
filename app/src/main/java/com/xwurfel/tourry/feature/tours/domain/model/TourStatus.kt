package com.xwurfel.tourry.feature.tours.domain.model

enum class TourStatus {
    PREPARING,  // Initial state, waiting for location permission/setup
    ACTIVE,     // Tour is running, tracking location
    PAUSED,     // User paused the tour
    COMPLETED   // Tour finished successfully
}