package org.phenoapps.intercross.data.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV6AddMetaIcon : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            try {
                beginTransaction()

                execSQL("ALTER TABLE metadata ADD COLUMN icon TEXT")
                execSQL("UPDATE metadata SET icon = '🍎' WHERE LOWER(property) = 'fruits'")
                execSQL("UPDATE metadata SET icon = '🌸' WHERE LOWER(property) = 'flowers'")
                execSQL("UPDATE metadata SET icon = '🌱' WHERE LOWER(property) = 'seeds'")

                setTransactionSuccessful()
            } catch (e: SQLiteException) {
                e.printStackTrace()
            } finally {
                endTransaction()
            }
        }
    }
}
