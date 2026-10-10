package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro histórico de una venta. Es un snapshot inmutable: unidadMedida,
 * precioUnitarioVenta y costoUnitarioVenta se copian del producto AL MOMENTO de
 * vender, para que el histórico (y las ganancias reportadas) no cambien si después
 * el producto cambia de precio, de costo o de unidad.
 *
 * v5: ya tiene llave foránea a Producto. Es segura porque los productos ya no se
 * borran de verdad (borrado suave con ProductoEntity.activo), así que una venta
 * nunca se queda apuntando a un producto inexistente. RESTRICT hace que un borrado
 * físico accidental de un producto con ventas falle en lugar de dejar huérfanas.
 */
@Entity(tableName = "venta",
        foreignKeys = {
                @ForeignKey(entity = ProductoEntity.class,
                        parentColumns = "id",
                        childColumns = "idProducto",
                        onDelete = ForeignKey.RESTRICT),
                @ForeignKey(entity = UsuarioEntity.class,
                        parentColumns = "id",
                        childColumns = "idUsuario",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("idProducto"), @Index("idUsuario"), @Index("timestampVentaMs")})
public class VentaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int idProducto;
    public double cantidadVendida;
    public UnidadMedida unidadMedida;
    public double precioUnitarioVenta;
    /** v5: costo del producto al momento de vender. Ganancia = (precio − costo) × cantidad. */
    public double costoUnitarioVenta;
    public double total;
    public long timestampVentaMs;
    /** v5: quién vendió (dueño o trabajador). Null en ventas anteriores a los roles. */
    public Integer idUsuario;
}
