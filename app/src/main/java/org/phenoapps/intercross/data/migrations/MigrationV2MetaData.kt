package org.phenoapps.intercross.data.migrations

import android.database.sqlite.SQLiteException
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration class for going from version 1 to 2
 * Version 2 added a metadata json encoded column
 */
class MigrationV2MetaData : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {

        with (database) {

            try {

                beginTransaction()

                createNewEventsTable()

                setTransactionSuccessful()

            } catch (e: SQLiteException) {

                e.printStackTrace()

            } finally {

                endTransaction()
            }
        }
    }

    //backup technique for deleting columns in SQLite https://www.sqlite.org/faq.html#q11
    private fun SupportSQLiteDatabase.createNewEventsTable() {

        execSQL("CREATE TEMP TABLE events_backup(codeId TEXT NOT NULL, mom TEXT NOT NULL, dad TEXT NOT NULL, name TEXT NOT NULL, date TEXT NOT NULL, person TEXT NOT NULL, experiment TEXT NOT NULL, type INTEGER NOT NULL, sex INTEGER NOT NULL, eid INTEGER PRIMARY KEY AUTOINCREMENT)")

        execSQL("INSERT INTO events_backup SELECT codeId, mom, dad, name, date, person, experiment, type, sex, eid FROM events")

        migrateStaticToTables()

        execSQL("DROP TABLE events")

        execSQL("""CREATE TABLE events(codeId TEXT NOT NULL, mom TEXT NOT NULL, dad TEXT NOT NULL, name TEXT NOT NULL, date TEXT NOT NULL, person TEXT NOT NULL, experiment TEXT NOT NULL, type INTEGER NOT NULL, sex INTEGER NOT NULL, eid INTEGER PRIMARY KEY AUTOINCREMENT)""")

        execSQL("INSERT INTO events SELECT codeId, mom, dad, name, date, person, experiment, type, sex, eid FROM events_backup")

        execSQL("DROP TABLE events_backup")

        execSQL("CREATE UNIQUE INDEX index_events_codeId ON events (codeId)")
    }

    //second transaction for inserting old static columns into encoded json column
    private fun SupportSQLiteDatabase.migrateStaticToTables() {

        execSQL("CREATE TABLE metadata(property TEXT NOT NULL, defaultValue INT DEFAULT NULL, mid INTEGER PRIMARY KEY AUTOINCREMENT)")

        execSQL("CREATE UNIQUE INDEX index_metadata_property ON metadata (property)")

        execSQL("CREATE TABLE metaValues(eid INTEGER NOT NULL, metaId INTEGER NOT NULL, value INT DEFAULT NULL, mvId INTEGER PRIMARY KEY AUTOINCREMENT)")

        execSQL("CREATE UNIQUE INDEX index_metaValues_codeId_mId ON metaValues (eid, metaId)")

        execSQL("INSERT INTO metadata (property, defaultValue, mid) VALUES ('flowers', 0, 1)")
        execSQL("INSERT INTO metadata (property, defaultValue, mid) VALUES ('seeds', 0, 2)")
        execSQL("INSERT INTO metadata (property, defaultValue, mid) VALUES ('fruits', 0, 3)")

        query("SELECT eid, fruits, seeds, flowers FROM events").use {

            with (it) {

                if (moveToFirst()) {

                    do {

                        //get indices for V1 static column string metadata and unique rowid
                        val idIndex = getColumnIndexOrThrow("eid")
                        val fruitsIndex = getColumnIndexOrThrow("fruits")
                        val seedsIndex = getColumnIndexOrThrow("seeds")
                        val flowersIndex = getColumnIndexOrThrow("flowers")

                        //get actual string values, eid is used to update the row and
                        //static metadata fields fruits/flowers/seeds will be encoded into a json string
                        val eid = getIntOrNull(idIndex) ?: "-1"
                        val fruits = getStringOrNull(fruitsIndex) ?: "0"
                        val flowers = getStringOrNull(flowersIndex) ?: "0"
                        val seeds = getStringOrNull(seedsIndex) ?: "0"

                        //creates a json encoded string from the static columns
                        execSQL(
                            "INSERT INTO metaValues (eid, metaId, value) VALUES (?, 1, ?)",
                            arrayOf(eid, flowers)
                        )
                        execSQL(
                            "INSERT INTO metaValues (eid, metaId, value) VALUES (?, 2, ?)",
                            arrayOf(eid, seeds)
                        )
                        execSQL(
                            "INSERT INTO metaValues (eid, metaId, value) VALUES (?, 3, ?)",
                            arrayOf(eid, fruits)
                        )

                    } while (moveToNext())
                }
            }
        }
    }
}