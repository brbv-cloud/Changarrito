package com.changarrito.database.dao;

import com.changarrito.database.entity.UnidadMedida;

/** Ganancia de un producto en un rango (JOIN venta + producto, agrupado por producto). */
public class GananciaProducto {
    public int idProducto;
    public String nombre;
    public UnidadMedida unidadMedida;
    public double cantidad;
    public double ventas;
    public double ventasConCosto;
    public double costo;
    public int ventasSinCosto;

    public boolean tieneCosto() {
        return ventasConCosto > 0 && costo > 0;
    }

    public double ganancia() {
        return ventasConCosto - costo;
    }
}
