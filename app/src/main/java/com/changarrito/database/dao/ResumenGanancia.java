package com.changarrito.database.dao;

/**
 * Totales de un rango de fechas para el reporte de Ganancias.
 * Solo las ventas con costo registrado (costoUnitarioVenta > 0) entran en la
 * ganancia; las demas se cuentan aparte para avisarle al dueno.
 */
public class ResumenGanancia {
    /** Todo lo vendido en el rango, tenga o no costo. */
    public double ventas;
    /** Lo vendido de productos que si tenian costo al momento de la venta. */
    public double ventasConCosto;
    /** Costo de lo vendido (solo ventas con costo). */
    public double costo;
    /** Cuantas ventas no tenian costo registrado. */
    public int ventasSinCosto;
    /** Importe de las ventas sin costo registrado. */
    public double importeSinCosto;
    public int transacciones;
}
