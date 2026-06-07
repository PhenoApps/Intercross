package org.phenoapps.intercross.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration class for going from version 4 to 5
 * Version 5 adds isArchived column to events table
 */
class MigrationV5ArchivedCrosses : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}
