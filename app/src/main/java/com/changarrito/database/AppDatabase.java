package com.changarrito.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ProductoEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public abstract ProductoDAO productoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            "changarrito_db"
                    ).addCallback(crearCallbackPrecarga(appContext)).build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback crearCallbackPrecarga(Context appContext) {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                databaseWriteExecutor.execute(() -> {
                    if (INSTANCE != null) {
                        INSTANCE.productoDao()
                                .insertAll(CatalogoBase.getProductosIniciales(appContext));
                    }
                });
            }
        };
    }
}