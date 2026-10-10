package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Registro de un arqueo/cierre de caja: compara lo que el sistema dice que se
 * vendio ese dia (efectivoSistema, calculado de la tabla Venta) contra lo que el
 * comerciante cuenta a mano (efectivoFisicoContado). Es un historico de auditoria,
 * no se edita despues de creado.
 */
@Entity(tableName = "cierre_caja")
public class CierreCajaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long fechaMs; // medianoche local del dia que se esta cerrando
    public double efectivoSistema;
    public double efectivoFisicoContado;
    public double diferencia; // efectivoFisicoContado - efectivoSistema (+ sobra / - falta)
    public String notas;
    public long timestampCreacionMs;
}
