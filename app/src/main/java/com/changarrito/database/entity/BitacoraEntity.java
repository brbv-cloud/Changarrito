package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bitácora de cambios: quién cambió qué, cuándo y qué valor tenía antes. Solo la ve
 * el dueño. Es de solo agregar: nunca se edita ni se borra una entrada.
 *
 * Una edición que cambia varios campos genera una entrada por campo (precio, costo,
 * existencias...), para que el dueño vea cada cambio por separado.
 * nombreProducto es una copia, por si después se renombra o se desactiva el producto.
 */
@Entity(tableName = "bitacora",
        foreignKeys = {
                @ForeignKey(entity = UsuarioEntity.class,
                        parentColumns = "id",
                        childColumns = "idUsuario",
                        onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = ProductoEntity.class,
                        parentColumns = "id",
                        childColumns = "idProducto",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("idUsuario"), @Index("idProducto"), @Index("timestampMs")})
public class BitacoraEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long timestampMs;
    public Integer idUsuario;
    public Integer idProducto;
    public String nombreProducto;
    public AccionBitacora accion;
    /** Campo que cambió ("precio", "costoCompra", "cantidadDisponible"...); null al crear o eliminar. */
    public String campo;
    public String valorAnterior;
    public String valorNuevo;
}
