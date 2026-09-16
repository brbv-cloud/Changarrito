package com.changarrito.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;

/**
 * Calcula el nivel de stock de un producto (semaforo verde/ambar/rojo) para que
 * las listas de inventario y venta lo comuniquen de un vistazo, sin tener que leer
 * el numero exacto de cantidad disponible.
 *
 * Umbral: se compara contra ProductoEntity.umbralStockBajo (capturado por el usuario
 * al crear el producto). Usa tolerancia EPSILON porque cantidadDisponible es double
 * (venta a granel) y comparar decimales con <= directo puede fallar por redondeo.
 */
public class EstadoStock {

    private static final double EPSILON = 0.0001;

    public enum Nivel { NORMAL, BAJO, AGOTADO }

    private EstadoStock() {
    }

    public static Nivel calcular(ProductoEntity producto) {
        if (producto == null) return Nivel.NORMAL;

        double cantidad = producto.cantidadDisponible;
        double umbral = producto.umbralStockBajo;

        if (cantidad <= EPSILON) return Nivel.AGOTADO;
        if (cantidad <= umbral + EPSILON) return Nivel.BAJO;
        return Nivel.NORMAL;
    }

    public static int colorRes(Nivel nivel) {
        switch (nivel) {
            case AGOTADO:
                return R.color.status_rojo;
            case BAJO:
                return R.color.status_ambar;
            case NORMAL:
            default:
                return R.color.status_verde;
        }
    }

    public static int color(Context context, ProductoEntity producto) {
        return ContextCompat.getColor(context, colorRes(calcular(producto)));
    }
}
