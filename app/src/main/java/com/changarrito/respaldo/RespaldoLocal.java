package com.changarrito.respaldo;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.RespaldoDAO;

/**
 * Lee y reemplaza la base de datos local (Room) completa. Sin internet ni Firebase:
 * por eso se puede probar con una BD en memoria (RespaldoLocalTest).
 */
public final class RespaldoLocal {

    private RespaldoLocal() {
    }

    public static RespaldoCompleto leer(AppDatabase db) {
        RespaldoDAO dao = db.respaldoDao();
        RespaldoCompleto r = new RespaldoCompleto();
        // Dentro de una transaccion: si se registra una venta a la mitad de la lectura,
        // la foto no queda con la venta pero sin el descuento de inventario.
        db.runInTransaction(() -> {
            r.configuracion = dao.leerConfiguracion();
            r.usuarios = dao.leerUsuarios();
            r.productos = dao.leerProductos();
            r.ventas = dao.leerVentas();
            r.cierres = dao.leerCierres();
            r.bitacora = dao.leerBitacora();
        });
        return r;
    }

    /**
     * Borra TODO lo local y lo reemplaza por el respaldo, en una sola transaccion:
     * si algo falla a la mitad, Room deshace todo y los datos de antes quedan igual.
     *
     * @param uidCuenta la cuenta con la que se inicio sesion; se fuerza en la
     *                  configuracion para que el celular quede ligado a esa cuenta.
     */
    public static void reemplazar(AppDatabase db, RespaldoCompleto r, String uidCuenta) {
        if (r == null || r.estaVacio()) {
            throw new IllegalArgumentException("El respaldo no tiene datos del negocio");
        }
        RespaldoDAO dao = db.respaldoDao();
        db.runInTransaction(() -> {
            dao.borrarAlertas();
            dao.borrarBitacora();
            dao.borrarVentas();
            dao.borrarCierres();
            dao.borrarProductos();
            dao.borrarUsuarios();
            dao.borrarConfiguracion();

            if (uidCuenta != null) r.configuracion.uidFirebase = uidCuenta;
            dao.insertarConfiguracion(r.configuracion);
            if (!r.usuarios.isEmpty()) dao.insertarUsuarios(r.usuarios);
            if (!r.productos.isEmpty()) dao.insertarProductos(r.productos);
            if (!r.ventas.isEmpty()) dao.insertarVentas(r.ventas);
            if (!r.cierres.isEmpty()) dao.insertarCierres(r.cierres);
            if (!r.bitacora.isEmpty()) dao.insertarBitacora(r.bitacora);
        });
    }
}
