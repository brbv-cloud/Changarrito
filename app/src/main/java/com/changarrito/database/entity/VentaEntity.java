package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro histórico de una venta. Es un snapshot inmutable: unidadMedida y
 * precioUnitarioVenta se copian del producto AL MOMENTO de vender, para que el
 * histórico no cambie si después el producto cambia de precio o de unidad.
 *
 * NOTA (decisión de diseño): idProducto es una referencia lógica al producto, sin
 * @ForeignKey. Con FK estricta, borrar un producto con ventas registradas lanzaría
 * SQLiteConstraintException y el botón "Eliminar" de ProductosActivity (que hoy no
 * maneja esa excepción) crashearía la app. Se deja como mejora futura: agregar la FK
 * junto con el manejo de error correspondiente en el flujo de borrado.
 */
@Entity(tableName = "venta", indices = {@Index("idProducto")})
public class VentaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int idProducto;
    public double cantidadVendida;
    public UnidadMedida unidadMedida;
    public double precioUnitarioVenta;
    public double total;
    public long timestampVentaMs;
    public String usuarioRegistro;
}
