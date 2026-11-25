package com.changarrito.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * NOTA (decision de diseno): igual que VentaEntity, idProducto es una referencia
 * logica sin @ForeignKey, por la misma razon (borrar un producto no debe poder
 * lanzar SQLiteConstraintException mientras ProductosActivity no maneje ese error).
 */
@Entity(tableName = "alerta", indices = {@Index("idProducto")})
public class AlertaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int idProducto;
    public TipoAlerta tipoAlerta;
    public EstadoAlerta estado;
    public long timestampCreacionMs;
    public long timestampResolucionMs;
    /**
     * v7: cuando termino el aviso (se surtio o cambio la fecha). 0 = sigue vigente, asi
     * que no se vuelve a avisar aunque ya se haya atendido. Las cerradas quedan como historial.
     */
    @ColumnInfo(defaultValue = "0")
    public long cerradaMs;
    /** v7: quien toco "Ya lo atendi" (nombre del turno). null = se cerro sola. */
    public String atendidaPor;
}
