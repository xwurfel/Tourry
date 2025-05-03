package com.xwurfel.tourry.data.poi.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pois")
data class PoiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imageUriString: String?,
    val latitude: Double,
    val longitude: Double
)