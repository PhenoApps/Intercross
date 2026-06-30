package org.phenoapps.intercross.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.phenoapps.intercross.data.dao.*
import org.phenoapps.intercross.data.migrations.MigrationV2MetaData
import org.phenoapps.intercross.data.migrations.MigrationV3WishlistView
import org.phenoapps.intercross.data.migrations.MigrationV4UniqueWishType
import org.phenoapps.intercross.data.migrations.MigrationV5DropSettings
import org.phenoapps.intercross.data.migrations.MigrationV6AddMetaIcon
import org.phenoapps.intercross.data.migrations.MigrationV7ArchiveEvents
import org.phenoapps.intercross.data.models.*
import kotlin.jvm.java

@Database(entities = [Event::class, Parent::class,
    Wishlist::class, PollenGroup::class,
    Meta::class, MetadataValues::class],
        views = [WishlistView::class], version = 7, exportSchema = true)
@TypeConverters(Converters::class)
abstract class IntercrossDatabase : RoomDatabase() {

    abstract fun eventsDao(): EventsDao
    abstract fun parentsDao(): ParentsDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun pollenGroupDao(): PollenGroupDao
    abstract fun metadataDao(): MetadataDao
    abstract fun metaValuesDao(): MetaValuesDao

    companion object {

        const val DATABASE_NAME = "INTERCROSS"

        //singleton pattern
        @Volatile private var instance: IntercrossDatabase? = null

        fun getInstance(ctx: Context): IntercrossDatabase {

            return instance ?: synchronized(this) {

                instance ?: buildDatabase(ctx).also { instance = it }
            }
        }

        private fun buildDatabase(ctx: Context): IntercrossDatabase {
            return Room.databaseBuilder(ctx, IntercrossDatabase::class.java, DATABASE_NAME)
                .addMigrations(MigrationV2MetaData()) //v1 -> v2 migration added JSON based metadata
                .addMigrations(MigrationV3WishlistView()) // v2 -> v3 migration for WishlistView
                .addMigrations(MigrationV4UniqueWishType()) // v3 -> v4 migration for unique wishlist type
                .addMigrations(MigrationV5DropSettings(ctx)) // v4 -> v5 migration drops settings table
                .addMigrations(MigrationV6AddMetaIcon()) // v5 -> v6 migration adds icon to metadata
                .addMigrations(MigrationV7ArchiveEvents()) // v6 -> v7 migration adds event archiving
                .setJournalMode(JournalMode.TRUNCATE) //truncate mode makes it easier to export/import database w/o having to manage WAL files.
                .build()
        }
    }
}
