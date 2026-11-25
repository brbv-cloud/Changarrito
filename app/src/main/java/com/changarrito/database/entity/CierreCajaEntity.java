package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Registro de un arqueo/cierre de caja: compara lo que el sistema dice que se
 * vendió desde el último cierre (efectivoSistema) contra lo que se contó a mano
 * (efectivoFisicoContado). Es un histórico de auditoría, no se edita después de creado.
 */
@Entity(tableName = "cierre_caja",
        foreignKeys = @ForeignKey(entity = UsuarioEntity.class,
                parentColumns = "id",
                childColumns = "idUsuario",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("idUsuario")})
public class CierreCajaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long fechaMs; // medianoche local del día que se está cerrando
    public double efectivoSistema;
    public double efectivoFisicoContado;
    public double diferencia; // efectivoFisicoContado - efectivoSistema (+ sobra / - falta)
    public String notas;
    public long timestampCreacionMs;
    /** v5: quién contó el efectivo. Si es trabajador, contó "a ciegas" (no vio efectivoSistema). */
    public Integer idUsuario;
}
