package com.xwurfel.tourry.data.poi.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xwurfel.tourry.data.poi.entity.PoiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoi(poiEntity: PoiEntity)

    @Query("SELECT * FROM pois")
    fun getPois(): Flow<List<PoiEntity>>

    @Query("SELECT * FROM pois WHERE id = :id")
    fun getPoiById(id: Long): Flow<PoiEntity?>

    @Query("SELECT * FROM pois WHERE latitude = :latitude AND longitude = :longitude")
    fun getPoiByCoordinates(latitude: Double, longitude: Double): Flow<PoiEntity?>

    @Query("DELETE FROM pois WHERE id = :id")
    suspend fun deletePoi(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM pois WHERE id = :id)")
    suspend fun isPoiSaved(id: Long): Boolean
}