package com.example.trackerwydatkow.wydatki;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Wydatki.class}, version = 1, exportSchema = false)
public abstract class WydatkiBaza extends RoomDatabase {
    private static WydatkiBaza instance;

    public abstract DAOWydatki DAOWydatki();

    public static synchronized WydatkiBaza getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            WydatkiBaza.class, "wydatki_baza")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}