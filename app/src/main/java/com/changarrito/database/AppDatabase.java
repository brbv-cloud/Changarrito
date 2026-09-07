package com.changarrito.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.VentaEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * v2 (Sprint 2): se agregó VentaEntity. Se usa fallbackToDestructiveMigration()
 * a propósito: el proyecto todavía no está lanzado (sin usuarios reales con datos
 * que proteger), así que no vale la pena escribir una Migration formal para cada
 * cambio de esquema durante el desarrollo activo. Antes del release final (Semana 6)
 * esto debe reemplazarse por una Migration real si ya hay datos de demo que conservar.
 */
@Database(entities = {ProductoEntity.class, VentaEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;
    private static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public abstract ProductoDAO productoDao();

    public abstract VentaDAO ventaDao();

    /**
     * SOLO PARA TESTS: reemplaza el singleton por una instancia inyectada (ej. una
     * BD en memoria), para que VentaRepository/ProductoRepository puedan probarse
     * sin tocar la BD real del dispositivo. Pasar null la limpia (usarlo en @After).
     */
    public static void setInstanceParaTest(AppDatabase instancia) {
        INSTANCE = instancia;
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            "changarrito_db"
                    ).addCallback(crearCallbackPrecarga(appContext))
                            .fallbackToDestructiveMigration()
                            .build();
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