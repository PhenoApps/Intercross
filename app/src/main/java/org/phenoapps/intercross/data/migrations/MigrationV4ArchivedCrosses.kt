package org.phenoapps.intercross.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration class for going from version 2 to 3
 * Version 4 adds isArchived column to events table
 */
class MigrationV4ArchivedCrosses : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}