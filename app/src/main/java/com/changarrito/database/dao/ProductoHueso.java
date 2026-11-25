package com.changarrito.database.dao;

import com.changarrito.database.entity.UnidadMedida;

/** Producto con existencia que no se ha vendido en mucho tiempo ("hueso"). */
public class ProductoHueso {
    public int idProducto;
    public String nombre;
    public double existencia;
    public UnidadMedida unidadMedida;
    public double costo;
    public double precio;
    /** Ultima venta; null si nunca se ha vendido. */
    public Long ultimaVentaMs;
    public long creadoMs;

    /** Desde cuando esta quieto: la ultima venta, o el alta si nunca se vendio. */
    public long quietoDesdeMs() {
        return ultimaVentaMs != null ? ultimaVentaMs : creadoMs;
    }

    /** Dinero detenido en anaquel (a costo). 0 si no tiene costo capturado. */
    public double dineroDetenido() {
        return costo > 0 ? existencia * costo : 0;
    }
}
