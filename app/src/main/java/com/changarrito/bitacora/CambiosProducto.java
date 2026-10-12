package com.changarrito.bitacora;

import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.ProductoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Compara un producto antes y después de guardarlo y arma las entradas de bitácora:
 * una por cada campo importante que cambió. Clase pura (sin Android) para probarla con JUnit.
 *
 * Solo se registran cambios hechos a mano desde el formulario. Las ventas también
 * bajan la existencia, pero esas ya quedan en la tabla de ventas con quién las hizo.
 */
public final class CambiosProducto {

    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_PRECIO = "precio";
    public static final String CAMPO_COSTO = "costoCompra";
    public static final String CAMPO_EXISTENCIA = "cantidadDisponible";
    public static final String CAMPO_CATEGORIA = "categoria";

    private static final double EPSILON = 0.0001;

    private CambiosProducto() {
    }

    public static List<BitacoraEntity> alCrear(ProductoEntity nuevo, Integer idUsuario, long ahoraMs) {
        List<BitacoraEntity> lista = new ArrayList<>();
        lista.add(entrada(nuevo, idUsuario, ahoraMs, AccionBitacora.CREAR_PRODUCTO, null, null, null));
        return lista;
    }

    public static BitacoraEntity alEliminar(ProductoEntity producto, Integer idUsuario, long ahoraMs) {
        return entrada(producto, idUsuario, ahoraMs, AccionBitacora.ELIMINAR_PRODUCTO, null, null, null);
    }

    public static List<BitacoraEntity> alEditar(ProductoEntity antes, ProductoEntity despues,
                                                Integer idUsuario, long ahoraMs) {
        List<BitacoraEntity> lista = new ArrayList<>();
        if (!Objects.equals(limpio(antes.nombre), limpio(despues.nombre))) {
            lista.add(entrada(despues, idUsuario, ahoraMs, AccionBitacora.EDITAR_PRODUCTO,
                    CAMPO_NOMBRE, limpio(antes.nombre), limpio(despues.nombre)));
        }
        if (distinto(antes.precio, despues.precio)) {
            lista.add(entrada(despues, idUsuario, ahoraMs, AccionBitacora.EDITAR_PRODUCTO,
                    CAMPO_PRECIO, dinero(antes.precio), dinero(despues.precio)));
        }
        if (distinto(antes.costoCompra, despues.costoCompra)) {
            lista.add(entrada(despues, idUsuario, ahoraMs, AccionBitacora.EDITAR_PRODUCTO,
                    CAMPO_COSTO, dinero(antes.costoCompra), dinero(despues.costoCompra)));
        }
        if (distinto(antes.cantidadDisponible, despues.cantidadDisponible)) {
            lista.add(entrada(despues, idUsuario, ahoraMs, AccionBitacora.AJUSTAR_EXISTENCIA,
                    CAMPO_EXISTENCIA, cantidad(antes.cantidadDisponible), cantidad(despues.cantidadDisponible)));
        }
        if (!Objects.equals(limpio(antes.categoria), limpio(despues.categoria))) {
            lista.add(entrada(despues, idUsuario, ahoraMs, AccionBitacora.EDITAR_PRODUCTO,
                    CAMPO_CATEGORIA, limpio(antes.categoria), limpio(despues.categoria)));
        }
        return lista;
    }

    private static BitacoraEntity entrada(ProductoEntity p, Integer idUsuario, long ahoraMs,
                                          AccionBitacora accion, String campo, String antes, String despues) {
        BitacoraEntity b = new BitacoraEntity();
        b.timestampMs = ahoraMs;
        b.idUsuario = idUsuario;
        b.idProducto = p.id > 0 ? p.id : null;
        b.nombreProducto = p.nombre;
        b.accion = accion;
        b.campo = campo;
        b.valorAnterior = antes;
        b.valorNuevo = despues;
        return b;
    }

    private static boolean distinto(double a, double b) {
        return Math.abs(a - b) > EPSILON;
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    static String dinero(double d) {
        return String.format(Locale.US, "%.2f", d);
    }

    static String cantidad(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.valueOf(Math.round(d * 1000) / 1000.0);
    }
}
