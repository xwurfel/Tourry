package com.xwurfel.tourry.domain.user_location.repository

import android.location.Location

interface UserLocationRepository {
    suspend fun getCurrentLocation(): Location?
}