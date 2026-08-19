package org.phenoapps.intercross.data.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV4UniqueWishType : Migration(3, 4) {

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            try {
                beginTransaction()

                createWishTypeIndex()

                setTransactionSuccessful()
            } catch (e: SQLiteException) {
                e.printStackTrace()
            } finally {
                endTransaction()
            }
        }
    }

    private fun SupportSQLiteDatabase.createWishTypeIndex() {
        execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_intercross_maleDbId_femaleDbId_wishType " +
                    "ON wishlist(maleDbId, femaleDbId, wishType)"
        )
    }
}