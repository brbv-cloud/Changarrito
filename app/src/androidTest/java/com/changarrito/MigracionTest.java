package com.changarrito;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.Migraciones;
import com.changarrito.database.entity.MermaEntity;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.database.entity.VentaEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Crea una base EXACTAMENTE como la v5 que ya tienen los celulares (copiada del
 * esquema que generaba Room), le mete datos y la abre con la app v6. Si la
 * migracion no deja el esquema como Room lo espera, Room truena y la prueba falla.
 */
@RunWith(AndroidJUnit4.class)
public class MigracionTest {

    private static final String NOMBRE = "migracion_v5_test.db";

    private static final String[] ESQUEMA_V5 = {
            "CREATE TABLE IF NOT EXISTS `producto` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT, `precio` REAL NOT NULL, `costoCompra` REAL NOT NULL, `margenPorcentaje` REAL NOT NULL, `categoria` TEXT, `cantidadDisponible` REAL NOT NULL, `unidadMedida` TEXT, `fechaCaducidadMs` INTEGER NOT NULL, `umbralStockBajo` REAL NOT NULL, `telefonoProveedor` TEXT, `barcode` TEXT, `activo` INTEGER NOT NULL DEFAULT 1, `timestampCreacionMs` INTEGER NOT NULL, `timestampActualizacionMs` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_producto_barcode` ON `producto` (`barcode`)",
            "CREATE INDEX IF NOT EXISTS `index_producto_activo` ON `producto` (`activo`)",
            "CREATE TABLE IF NOT EXISTS `venta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idProducto` INTEGER NOT NULL, `cantidadVendida` REAL NOT NULL, `unidadMedida` TEXT, `precioUnitarioVenta` REAL NOT NULL, `costoUnitarioVenta` REAL NOT NULL, `total` REAL NOT NULL, `timestampVentaMs` INTEGER NOT NULL, `idUsuario` INTEGER, FOREIGN KEY(`idProducto`) REFERENCES `producto`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_venta_idProducto` ON `venta` (`idProducto`)",
            "CREATE INDEX IF NOT EXISTS `index_venta_idUsuario` ON `venta` (`idUsuario`)",
            "CREATE INDEX IF NOT EXISTS `index_venta_timestampVentaMs` ON `venta` (`timestampVentaMs`)",
            "CREATE TABLE IF NOT EXISTS `alerta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idProducto` INTEGER NOT NULL, `tipoAlerta` TEXT, `estado` TEXT, `timestampCreacionMs` INTEGER NOT NULL, `timestampResolucionMs` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_alerta_idProducto` ON `alerta` (`idProducto`)",
            "CREATE TABLE IF NOT EXISTS `cierre_caja` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fechaMs` INTEGER NOT NULL, `efectivoSistema` REAL NOT NULL, `efectivoFisicoContado` REAL NOT NULL, `diferencia` REAL NOT NULL, `notas` TEXT, `timestampCreacionMs` INTEGER NOT NULL, `idUsuario` INTEGER, FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_cierre_caja_idUsuario` ON `cierre_caja` (`idUsuario`)",
            "CREATE TABLE IF NOT EXISTS `usuario` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT, `rol` TEXT, `claveHash` TEXT, `claveSal` TEXT, `intentosFallidos` INTEGER NOT NULL DEFAULT 0, `bloqueadoHastaMs` INTEGER NOT NULL DEFAULT 0, `activo` INTEGER NOT NULL DEFAULT 1, `timestampCreacionMs` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `bitacora` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestampMs` INTEGER NOT NULL, `idUsuario` INTEGER, `idProducto` INTEGER, `nombreProducto` TEXT, `accion` TEXT, `campo` TEXT, `valorAnterior` TEXT, `valorNuevo` TEXT, FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`idProducto`) REFERENCES `producto`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_bitacora_idUsuario` ON `bitacora` (`idUsuario`)",
            "CREATE INDEX IF NOT EXISTS `index_bitacora_idProducto` ON `bitacora` (`idProducto`)",
            "CREATE INDEX IF NOT EXISTS `index_bitacora_timestampMs` ON `bitacora` (`timestampMs`)",
            "CREATE TABLE IF NOT EXISTS `configuracion_negocio` (`id` INTEGER NOT NULL, `uidFirebase` TEXT, `nombreNegocio` TEXT, `giro` TEXT, `unidadesHabilitadas` TEXT, `modoCaducidad` TEXT, `margenPorDefecto` REAL NOT NULL, `catalogoImportado` INTEGER NOT NULL, `timestampActualizacionMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4503210eb32df0a41daf6675fdc5ff6b')"
    };

    private Context ctx;

    @Before
    public void crearBaseV5() {
        ctx = ApplicationProvider.getApplicationContext();
        ctx.deleteDatabase(NOMBRE);
        SQLiteDatabase v5 = SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(NOMBRE), null);
        for (String sql : ESQUEMA_V5) v5.execSQL(sql);

        ContentValues p = new ContentValues();
        p.put("id", 1);
        p.put("nombre", "Coca-Cola 600 ml");
        p.put("precio", 17.0);
        p.put("costoCompra", 12.0);
        p.put("margenPorcentaje", 40.0);
        p.put("cantidadDisponible", 10.0);
        p.put("unidadMedida", "PZA");
        p.put("fechaCaducidadMs", 0);
        p.put("umbralStockBajo", 2.0);
        p.put("timestampCreacionMs", 1);
        p.put("timestampActualizacionMs", 1);
        v5.insert("producto", null, p);

        for (long ts : new long[]{1000L, 2000L}) {
            ContentValues v = new ContentValues();
            v.put("idProducto", 1);
            v.put("cantidadVendida", 1.0);
            v.put("unidadMedida", "PZA");
            v.put("precioUnitarioVenta", 17.0);
            v.put("costoUnitarioVenta", 12.0);
            v.put("total", 17.0);
            v.put("timestampVentaMs", ts);
            v5.insert("venta", null, v);
        }
        v5.setVersion(5);
        v5.close();
    }

    @After
    public void borrar() {
        ctx.deleteDatabase(NOMBRE);
    }

    @Test
    public void migra_de_v5_a_v6_sin_perder_ventas() {
        AppDatabase db = Room.databaseBuilder(ctx, AppDatabase.class, NOMBRE)
                .addMigrations(Migraciones.MIGRACION_5_6)
                .allowMainThreadQueries()
                .build();
        try {
            // Abrir la base dispara la migracion y la validacion del esquema.
            List<VentaEntity> ventas = db.respaldoDao().leerVentas();
            assertEquals(2, ventas.size());
            assertEquals(1000L, ventas.get(0).idTicket);   // cada venta vieja = su propio ticket
            assertEquals(2000L, ventas.get(1).idTicket);
            assertEquals(1, db.respaldoDao().leerProductos().size());

            MermaEntity m = new MermaEntity();
            m.idProducto = 1;
            m.nombreProducto = "Coca-Cola 600 ml";
            m.cantidad = 2;
            m.costoUnitario = 12;
            m.motivo = MotivoMerma.CADUCADO;
            m.timestampMs = 3000;
            db.mermaDao().insert(m);
            assertEquals(1, db.respaldoDao().leerMermas().size());
        } finally {
            db.close();
        }
    }
}
