package org.phenoapps.intercross.data.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV7ArchiveEvents : Migration(6, 7) {

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            try {
                beginTransaction()

                execSQL("ALTER TABLE events ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")

                setTransactionSuccessful()
            } catch (e: SQLiteException) {
                e.printStackTrace()
            } finally {
                endTransaction()
            }
        }
    }
}
