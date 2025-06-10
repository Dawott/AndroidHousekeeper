package com.example.trackerwydatkow.wydatki;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Wydatki.class}, version = 3, exportSchema = false)
public abstract class WydatkiBaza extends RoomDatabase {
    private static WydatkiBaza instance;

    public abstract DAOWydatki DAOWydatki();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE wydatki ADD COLUMN waluta TEXT DEFAULT 'PLN'");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE wydatki ADD COLUMN receipt_image_url TEXT");
            database.execSQL("ALTER TABLE wydatki ADD COLUMN is_from_receipt INTEGER DEFAULT 0");
            database.execSQL("ALTER TABLE wydatki ADD COLUMN ocr_confidence REAL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_3_3 = new Migration(3, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
        }
    };

    public static synchronized WydatkiBaza getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            WydatkiBaza.class, "wydatki_baza")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}