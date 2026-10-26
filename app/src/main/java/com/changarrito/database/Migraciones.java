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
}
