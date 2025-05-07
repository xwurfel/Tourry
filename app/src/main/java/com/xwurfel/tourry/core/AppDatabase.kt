package com.xwurfel.tourry.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.entity.BookingEntity
import com.xwurfel.tourry.data.category.dao.TourCategoryDao
import com.xwurfel.tourry.data.category.entity.TourCategoryEntity
import com.xwurfel.tourry.data.checkin.dao.CheckInDao
import com.xwurfel.tourry.data.checkin.entity.CheckInEntity
import com.xwurfel.tourry.data.poi.dao.PoiDao
import com.xwurfel.tourry.data.poi.entity.PoiEntity
import com.xwurfel.tourry.data.route.dao.RoutePointDao
import com.xwurfel.tourry.data.route.entity.RoutePointEntity
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.SyncTypeConverters
import com.xwurfel.tourry.data.sync.dao.SyncDao
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
        TourCategoryEntity::class,
        RoutePointEntity::class,
        CheckInEntity::class,
        SyncEntity::class
    ],
    version = 6
)
@TypeConverters(DateTimeConverters::class, SyncTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao
    abstract fun userDao(): UserDao
    abstract fun tourDao(): TourDao
    abstract fun bookingDao(): BookingDao
    abstract fun tourCategoryDao(): TourCategoryDao
    abstract fun routePointDao(): RoutePointDao
    abstract fun checkInDao(): CheckInDao
    abstract fun syncDao(): SyncDao
}