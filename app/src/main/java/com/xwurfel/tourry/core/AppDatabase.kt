package com.xwurfel.tourry.core

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.entity.PoiEntity

@Database(entities = [PoiEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao
}