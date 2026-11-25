package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Merma: mercancia que salio del inventario sin venderse (danada, caducada,
 * robada...). Descuenta la existencia y guarda el costo de ese momento para
 * saber cuanto dinero se perdio. Modelo v6.
 */
@Entity(tableName = "merma",
        foreignKeys = {
                @ForeignKey(entity = ProductoEntity.class,
                        parentColumns = "id",
                        childColumns = "idProducto",
                        onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = UsuarioEntity.class,
                        parentColumns = "id",
                        childColumns = "idUsuario",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("idProducto"), @Index("idUsuario"), @Index("timestampMs")})
public class MermaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Integer idProducto;
    /** Copia del nombre: si el producto se borra, el registro sigue diciendo que era. */
    public String nombreProducto;
    public double cantidad;
    public UnidadMedida unidadMedida;
    /** Costo por unidad al momento de la merma (0 si el producto no tenia costo). */
    public double costoUnitario;
    public MotivoMerma motivo;
    public String nota;
    public long timestampMs;
    public Integer idUsuario;

    public double costoTotal() {
        return cantidad * costoUnitario;
    }
}
