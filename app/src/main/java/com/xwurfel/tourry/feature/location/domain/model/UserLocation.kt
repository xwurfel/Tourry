package com.xwurfel.tourry.feature.location.domain.model

import android.location.Location

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isAccurate: Boolean get() = accuracy <= 20f // Within 20 meters

    fun distanceTo(latitude: Double, longitude: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            this.latitude, this.longitude,
            latitude, longitude,
            results
        )
        return results[0]
    }
}