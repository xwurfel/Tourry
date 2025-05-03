package com.xwurfel.tourry.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.entity.BookingEntity
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.entity.TourCategoryEntity
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.entity.PoiEntity
import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.tour.entity.TourEntity
import com.xwurfel.tourry.data.user.dao.UserDao
import com.xwurfel.tourry.data.user.entity.UserEntity
import com.xwurfel.tourry.util.converters.DateTimeConverters

@Database(
    entities = [
        PoiEntity::class,
        UserEntity::class,
        TourEntity::class,
        BookingEntity::class,
        TourCategoryEntity::class
    ],
    version = 2
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao
    abstract fun userDao(): UserDao
    abstract fun tourDao(): TourDao
    abstract fun bookingDao(): BookingDao
    abstract fun tourCategoryDao(): TourCategoryDao
}