package com.xwurfel.tourry.core.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_actions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entityType` TEXT NOT NULL,
                `entityId` INTEGER NOT NULL,
                `actionType` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `actionData` TEXT
            )
            """
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_actions_type` ON `sync_actions` (`actionType`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_actions_entity` ON `sync_actions` (`entityType`, `entityId`)"
        )
    }
}