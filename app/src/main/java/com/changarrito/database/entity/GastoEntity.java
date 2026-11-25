package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Gasto del negocio (renta, luz, agua, sueldos...). Se resta de la ganancia en Reportes
 * para saber cuanto quedo de verdad. Si se pago con el efectivo de la caja, tambien se
 * resta de lo que debe haber al cerrar caja. Modelo v8.
 */
@Entity(tableName = "gasto",
        foreignKeys = @ForeignKey(entity = UsuarioEntity.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("idUsuario"), @Index("timestampMs")})
public class GastoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public TipoGasto tipo;
    public double monto;
    public String nota;
    /** Salio del cajon: el cierre de caja lo descuenta del efectivo esperado. */
    public boolean pagadoConCaja;
    /** Cuando se pago (el dia que cuenta en Reportes). */
    public long timestampMs;
    /** Quien lo registro (null si fue sin turno). */
    public Integer idUsuario;
}
