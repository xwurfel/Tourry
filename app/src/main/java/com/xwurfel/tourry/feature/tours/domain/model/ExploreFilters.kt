package com.xwurfel.tourry.feature.tours.domain.model

data class ExploreFilters(
    val themes: List<String> = emptyList(),
    val dateRange: Pair<Long, Long>? = null,
    val maxDuration: Int? = null, // in hours
    val maxPrice: Double? = null,
    val maxDistance: Float? = null // in km
) {
    fun isEmpty(): Boolean {
        return themes.isEmpty() &&
                dateRange == null &&
                maxDuration == null &&
                maxPrice == null &&
                maxDistance == null
    }
}