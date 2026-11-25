package com.changarrito.database.dao;

/**
 * Ventas, costo y ganancia de un dia (consulta agrupada por fecha local).
 * Solo trae dias CON ventas; los dias sin ventas los rellena SerieDiaria.
 */
public class GananciaDia {
    /** "yyyy-MM-dd" en la zona horaria del celular. */
    public String fecha;
    public double ventas;
    public double ventasConCosto;
    public double costo;
}
