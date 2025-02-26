package org.phenoapps.intercross.data.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration class for going from version 2 to 3
 * Version 3 ensures WishlistView is correctly established
 */
class MigrationV3WishlistView : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        with(db) {
            try {
                beginTransaction()

                createWishlistView()

                setTransactionSuccessful()
            } catch (e: SQLiteException) {
                e.printStackTrace()
            } finally {
                endTransaction()
            }
        }
    }

    private fun SupportSQLiteDatabase.createWishlistView() {
        // drop the old view
        execSQL("DROP VIEW IF EXISTS WishlistView")

        // create the new view with updated logic for wish progress calculation
        execSQL("CREATE VIEW `WishlistView` AS SELECT DISTINCT femaleDbId as momId, femaleName as momName, maleDbId as dadId, maleName as dadName, wishMin, wishMax, wishType,\n" +
                "\t(SELECT COUNT(*) \n" +
                "\tFROM events as child\n" +
                "\tWHERE (w.femaleDbId = child.mom and ((w.maleDbId = child.dad) or (child.dad = \"blank\" and w.maleDbId = \"-1\")))) as wishProgress\n" +
                "from wishlist as w")
    }
}