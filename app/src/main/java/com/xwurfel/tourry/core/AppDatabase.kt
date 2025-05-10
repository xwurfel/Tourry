package com.xwurfel.tourry.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xwurfel.tourry.util.converters.DateTimeConverters

@Database(
    entities = [],
    version = 1
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {

}