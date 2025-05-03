package com.xwurfel.tourry.data.poi.repository

import com.xwurfel.tourry.data.poi.mapper.toDomain
import com.xwurfel.tourry.data.poi.mapper.toEntity
import com.xwurfel.tourry.data.poi.source.PoiDataSource
import com.xwurfel.tourry.domain.poi.model.PointOfInterest
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PoiRepositoryImpl @Inject constructor(
    private val poiDataSource: PoiDataSource
) : PoiRepository {
    override suspend fun savePoi(poi: PointOfInterest) {
        poiDataSource.savePoi(poi.toEntity())
    }

    override fun getPois(): Flow<List<PointOfInterest>> {
        return poiDataSource.getPois().map { pois ->
            pois.map { it.toDomain() }
        }
    }

    override suspend fun deletePoi(id: Long) {
        poiDataSource.deletePoi(id)
    }

    override fun getPoiById(id: Long): Flow<PointOfInterest?> {
        return poiDataSource.getPoiById(id).map { it?.toDomain() }
    }

    override suspend fun isPoiSaved(id: Long): Boolean {
        return poiDataSource.isPoiSaved(id)
    }

    override fun getPoiByCoordinates(latitude: Double, longitude: Double): Flow<PointOfInterest?> {
        return poiDataSource.getPoiByCoordinates(latitude, longitude).map { it?.toDomain() }
    }
}