package com.xwurfel.tourry.domain.poi.repository

import com.xwurfel.tourry.domain.poi.model.PointOfInterest
import kotlinx.coroutines.flow.Flow

interface PoiRepository {
    suspend fun savePoi(poi: PointOfInterest)

    fun getPois(): Flow<List<PointOfInterest>>

    suspend fun deletePoi(id: Long)

    fun getPoiById(id: Long): Flow<PointOfInterest?>

    suspend fun isPoiSaved(id: Long): Boolean

    fun getPoiByCoordinates(latitude: Double, longitude: Double): Flow<PointOfInterest?>
}