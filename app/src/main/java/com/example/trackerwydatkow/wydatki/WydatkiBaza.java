package com.example.trackerwydatkow.wydatki;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Wydatki.class}, version = 2, exportSchema = false)
public abstract class WydatkiBaza extends RoomDatabase {
    private static WydatkiBaza instance;

    public abstract DAOWydatki DAOWydatki();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE wydatki ADD COLUMN waluta TEXT DEFAULT 'PLN'");
        }
    };

    public static synchronized WydatkiBaza getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            WydatkiBaza.class, "wydatki_baza")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}