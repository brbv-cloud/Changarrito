package com.changarrito.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "producto", indices = {@Index("barcode"), @Index("activo")})
public class ProductoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    /** Precio de venta por unidad de medida (por pieza, por KG, por metro...). */
    public double precio;
    /** v5: lo que le cuesta al negocio cada unidad. Base del cálculo de ganancia. */
    public double costoCompra;
    /**
     * v5: % de ganancia sobre el costo que el usuario eligió al capturar el producto.
     * Precio sugerido = costo × (1 + margen / 100). Si el usuario edita el precio a
     * mano, este valor se recalcula (ver utils.CalculoPrecio).
     */
    public double margenPorcentaje;
    /** v5: categoría libre ("Bebidas", "Tornillería"...). Las sugeridas dependen del giro. */
    public String categoria;
    public double cantidadDisponible;
    public UnidadMedida unidadMedida;
    public long fechaCaducidadMs;
    public double umbralStockBajo;
    public String telefonoProveedor;
    public String barcode;
    /**
     * v5: borrado suave. "Eliminar" un producto solo lo marca inactivo, para que las
     * ventas viejas sigan apuntando a él (Venta tiene llave foránea a Producto) y los
     * reportes históricos no pierdan el nombre. Las listas solo muestran activos.
     */
    @ColumnInfo(defaultValue = "1")
    public boolean activo = true;
    public long timestampCreacionMs;
    public long timestampActualizacionMs;
}
