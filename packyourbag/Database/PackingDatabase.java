
package com.example.packyourbag.Database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.packyourbag.DAOInterfaces.TripDao;
import com.example.packyourbag.DAOInterfaces.PackingItemDao;
import com.example.packyourbag.DatabaseEntities.PackingItem;
import com.example.packyourbag.DatabaseEntities.Trip;

@Database(entities = {Trip.class, PackingItem.class}, version = 2, exportSchema = false)
public abstract class PackingDatabase extends RoomDatabase {
    public abstract TripDao tripDao();
    public abstract PackingItemDao packingItemDao();

    // Migration from version 1 to 2 (adding createdAt field to packing_items)
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add createdAt column to packing_items table
            database.execSQL("ALTER TABLE packing_items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0");
        }
    };
}