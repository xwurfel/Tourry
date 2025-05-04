package com.xwurfel.tourry.core.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `check_ins` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `tourId` INTEGER NOT NULL,
                `routePointId` INTEGER NOT NULL,
                `timestamp` TEXT NOT NULL,
                `note` TEXT,
                `imageUriString` TEXT,
                FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`tourId`) REFERENCES `tours`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`routePointId`) REFERENCES `route_points`(`id`) ON DELETE CASCADE
            )
            """
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_check_ins_userId` ON `check_ins` (`userId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_check_ins_tourId` ON `check_ins` (`tourId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_check_ins_routePointId` ON `check_ins` (`routePointId`)"
        )
    }
}