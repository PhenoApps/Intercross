package org.phenoapps.intercross.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.phenoapps.intercross.data.dao.*
import org.phenoapps.intercross.data.migrations.MigrationV2MetaData
import org.phenoapps.intercross.data.models.*

@Database(entities = [Event::class, Parent::class,
    Wishlist::class, Settings::class, PollenGroup::class,
    Meta::class, MetadataValues::class],
        views = [WishlistView::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class IntercrossDatabase : RoomDatabase() {

    abstract fun eventsDao(): EventsDao
    abstract fun parentsDao(): ParentsDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun settingsDao(): SettingsDao
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
                .addMigrations(MigrationV2MetaData(), MigrationV2ToV3())
                .fallbackToDestructiveMigration()
                .setJournalMode(JournalMode.TRUNCATE)
                .build()
        }
    }
}

class MigrationV2ToV3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // If no schema changes are needed, you can leave this empty
        // This will just bump the version number
    }
}
