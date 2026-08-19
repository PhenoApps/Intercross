package org.phenoapps.intercross.data.migrations

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.phenoapps.intercross.util.CrossIdSettings

/**
 * Migration from v4 to v5: moves Settings table data into SharedPreferences
 * and drops the settings table.
 */
class MigrationV5DropSettings(private val context: Context) : Migration(4, 5) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // Read existing settings before dropping
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            db.query("SELECT * FROM settings LIMIT 1").use { cursor ->
                if (cursor.moveToFirst()) {
                    val isPattern = cursor.getInt(cursor.getColumnIndexOrThrow("isPattern")) == 1
                    val isUUID = cursor.getInt(cursor.getColumnIndexOrThrow("isUUID")) == 1
                    val isAutoIncrement = cursor.getInt(cursor.getColumnIndexOrThrow("isAutoIncrement")) == 1
                    val pad = cursor.getInt(cursor.getColumnIndexOrThrow("pad"))
                    val number = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
                    val prefix = cursor.getString(cursor.getColumnIndexOrThrow("prefix")) ?: ""
                    val suffix = cursor.getString(cursor.getColumnIndexOrThrow("suffix")) ?: ""
                    CrossIdSettings.save(
                        prefs,
                        CrossIdSettings(isPattern, isUUID, isAutoIncrement, pad, number, prefix, suffix)
                    )
                }
            }
        } catch (_: Exception) {
            // If the table doesn't exist or is empty, just proceed with defaults
        }
        db.execSQL("DROP TABLE IF EXISTS settings")

        // Fix stale index from MigrationV4 that has wrong column order/name
        db.execSQL("DROP INDEX IF EXISTS index_intercross_maleDbId_femaleDbId_wishType")
        // Ensure the correct index exists
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_wishlist_femaleDbId_maleDbId_wishType ON wishlist(femaleDbId, maleDbId, wishType)")
    }
}
