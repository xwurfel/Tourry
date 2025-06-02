package com.xwurfel.tourry.feature.tours.domain.model

data class RouteDeviation(
    val isDeviated: Boolean,
    val distanceFromRoute: Double,
    val nearestStopName: String?
) {
    val severityLevel: DeviationSeverity
        get() = when {
            !isDeviated -> DeviationSeverity.NONE
            distanceFromRoute < 100 -> DeviationSeverity.MINOR
            distanceFromRoute < 300 -> DeviationSeverity.MODERATE
            else -> DeviationSeverity.MAJOR
        }
}