package com.changarrito.database.dao;

import com.changarrito.database.entity.UnidadMedida;

/**
 * Resultado de la consulta "productos más vendidos" (JOIN venta + producto,
 * agrupado por producto). No es una entidad de BD, solo un POJO de proyección.
 */
public class ProductoConTotal {
    public String nombre;
    public double cantidadVendida;
    public UnidadMedida unidadMedida;
    public double totalIngresos;
}
