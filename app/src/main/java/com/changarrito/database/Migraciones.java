package com.changarrito.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migraciones reales (sin borrar datos). A partir de v6 ya hay negocios con datos
 * de prueba que no se deben perder al actualizar la app.
 *
 * El SQL tiene que coincidir EXACTO con lo que Room espera de las entidades
 * (columnas, NOT NULL, DEFAULT, llaves foraneas e indices); si no, Room lo detecta
 * al abrir la base y truena. MigracionTest lo prueba con una base v5 real.
 */
public final class Migraciones {

    private Migraciones() {
    }

    /** v5 → v6: numero de ticket en cada venta + tabla de mermas. */
    public static final Migration MIGRACION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Ticket: las ventas viejas no sabian a que compra pertenecian, asi que
            // cada una queda como su propio ticket (se usa su hora como numero).
            db.execSQL("ALTER TABLE `venta` ADD COLUMN `idTicket` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE `venta` SET `idTicket` = `timestampVentaMs`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_venta_idTicket` ON `venta` (`idTicket`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `merma` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`idProducto` INTEGER, "
                    + "`nombreProducto` TEXT, "
                    + "`cantidad` REAL NOT NULL, "
                    + "`unidadMedida` TEXT, "
                    + "`costoUnitario` REAL NOT NULL, "
                    + "`motivo` TEXT, "
                    + "`nota` TEXT, "
                    + "`timestampMs` INTEGER NOT NULL, "
                    + "`idUsuario` INTEGER, "
                    + "FOREIGN KEY(`idProducto`) REFERENCES `producto`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , "
                    + "FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_merma_idProducto` ON `merma` (`idProducto`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_merma_idUsuario` ON `merma` (`idUsuario`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_merma_timestampMs` ON `merma` (`timestampMs`)");
        }
    };

    /**
     * v6 → v7: productos "pendientes de surtir" y registro de alertas atendidas.
     * Se marcan como ya surtidos los productos que hoy tienen existencia o que alguna
     * vez se vendieron o se registraron como merma.
     */
    public static final Migration MIGRACION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `producto` ADD COLUMN `surtidoMs` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE `producto` SET `surtidoMs` = MAX(`timestampActualizacionMs`, 1) "
                    + "WHERE `cantidadDisponible` > 0 "
                    + "OR `id` IN (SELECT `idProducto` FROM `venta`) "
                    + "OR `id` IN (SELECT `idProducto` FROM `merma` WHERE `idProducto` IS NOT NULL)");
            db.execSQL("ALTER TABLE `alerta` ADD COLUMN `cerradaMs` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `alerta` ADD COLUMN `atendidaPor` TEXT");
        }
    };

    /** v7 → v8: gastos del negocio (renta, luz, sueldos, lo pagado con la caja). */
    public static final Migration MIGRACION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `gasto` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`tipo` TEXT, "
                    + "`monto` REAL NOT NULL, "
                    + "`nota` TEXT, "
                    + "`pagadoConCaja` INTEGER NOT NULL, "
                    + "`timestampMs` INTEGER NOT NULL, "
                    + "`idUsuario` INTEGER, "
                    + "FOREIGN KEY(`idUsuario`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gasto_idUsuario` ON `gasto` (`idUsuario`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gasto_timestampMs` ON `gasto` (`timestampMs`)");
        }
    };
}
