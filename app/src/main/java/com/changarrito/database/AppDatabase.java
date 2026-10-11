package com.changarrito.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.changarrito.database.dao.AlertaDAO;
import com.changarrito.database.dao.BitacoraDAO;
import com.changarrito.database.dao.CierreCajaDAO;
import com.changarrito.database.dao.ConfiguracionNegocioDAO;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.dao.UsuarioDAO;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.AlertaEntity;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;

/**
 * Sprint 6: ya no se precarga un catálogo fijo al crear la BD. El dueño elige su
 * giro y decide qué importar en la vista previa (negocio.CatalogoGiro).
 *
 * v5 (Sprint 5, modelo para login, giros, ganancias, roles y bitácora):
 * - Producto: costoCompra, margenPorcentaje, categoria, activo (borrado suave).
 * - Venta: costoUnitarioVenta, idUsuario y llave foránea a Producto.
 * - CierreCaja: idUsuario.
 * - Tablas nuevas: Usuario, Bitacora, ConfiguracionNegocio.
 * - UnidadMedida: sale LT, entra M (metro).
 *
 * v4 (Sprint 4): se agregó CierreCajaEntity. Se usa fallbackToDestructiveMigration()
 * a propósito: el proyecto todavía no está lanzado (sin usuarios reales con datos
 * que proteger), así que no vale la pena escribir una Migration formal para cada
 * cambio de esquema durante el desarrollo activo. Antes del release final (Semana 6)
 * esto debe reemplazarse por una Migration real si ya hay datos de demo que conservar.
 */
@Database(entities = {
        ProductoEntity.class,
        VentaEntity.class,
        AlertaEntity.class,
        CierreCajaEntity.class,
        UsuarioEntity.class,
        BitacoraEntity.class,
        ConfiguracionNegocioEntity.class
}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract ProductoDAO productoDao();

    public abstract VentaDAO ventaDao();

    public abstract AlertaDAO alertaDao();

    public abstract CierreCajaDAO cierreCajaDao();

    public abstract UsuarioDAO usuarioDao();

    public abstract BitacoraDAO bitacoraDao();

    public abstract ConfiguracionNegocioDAO configuracionNegocioDao();

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
                    ).fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
