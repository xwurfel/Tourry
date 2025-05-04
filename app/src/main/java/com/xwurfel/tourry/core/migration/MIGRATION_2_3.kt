package com.xwurfel.tourry.core.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 2 to 3.
 * Adds the route_points table for tour routes.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the route_points table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `route_points` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tourId` INTEGER NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `order` INTEGER NOT NULL,
                `durationMinutes` INTEGER,
                `arrivalInstructions` TEXT,
                `imageUriString` TEXT,
                FOREIGN KEY(`tourId`) REFERENCES `tours`(`id`) ON DELETE CASCADE
            )
            """
        )

        // Create an index for faster querying
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_route_points_tourId` ON `route_points` (`tourId`)"
        )
    }
}