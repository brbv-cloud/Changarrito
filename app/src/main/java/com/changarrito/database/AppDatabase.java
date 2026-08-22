package com.changarrito.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

@Database(entities = {ProductoEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract ProductoDAO productoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "changarrito_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
} 