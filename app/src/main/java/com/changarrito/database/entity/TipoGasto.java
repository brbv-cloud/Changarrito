package com.changarrito.database.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * De que es un gasto del negocio. Lo que se compra para revender NO es gasto: eso ya
 * esta en el costo de cada producto (si se anotara aqui, la ganancia saldria contada dos veces).
 *
 * Renta y sueldos solo los registra el dueno; el trabajador solo anota lo que saca de la caja.
 */
public enum TipoGasto {
    RENTA("Renta", true),
    LUZ("Luz", false),
    AGUA("Agua", false),
    GAS("Gas", false),
    SUELDOS("Sueldos", true),
    INSUMOS("Bolsas y limpieza", false),
    OTRO("Otro", false);

    public final String nombreVisible;
    /** true = solo el dueno lo puede registrar (y ver). */
    public final boolean soloDueno;

    TipoGasto(String nombreVisible, boolean soloDueno) {
        this.nombreVisible = nombreVisible;
        this.soloDueno = soloDueno;
    }

    /** Los tipos que puede elegir quien esta en turno. */
    public static List<TipoGasto> disponibles(boolean esDueno) {
        List<TipoGasto> lista = new ArrayList<>();
        for (TipoGasto t : values()) {
            if (esDueno || !t.soloDueno) lista.add(t);
        }
        return lista;
    }

    /** Nombre para mostrar a partir del texto guardado; tolera valores desconocidos. */
    public static String nombre(String guardado) {
        if (guardado == null) return OTRO.nombreVisible;
        for (TipoGasto t : values()) {
            if (t.name().equals(guardado)) return t.nombreVisible;
        }
        return guardado;
    }
}
