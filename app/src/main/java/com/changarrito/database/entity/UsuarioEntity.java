package com.changarrito.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Persona que usa el celular del negocio (un solo dispositivo por negocio).
 * El dueño inicia sesión con su cuenta de Firebase; cada quien, al empezar su
 * turno, elige su perfil y pone su clave de 4 dígitos.
 *
 * La clave NUNCA se guarda en texto: se guarda un hash PBKDF2 con una sal aleatoria
 * por usuario (ver utils.ClaveUtil). intentosFallidos y bloqueadoHastaMs sirven
 * para frenar a quien intente adivinar la clave probando combinaciones.
 */
@Entity(tableName = "usuario")
public class UsuarioEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public Rol rol;
    public String claveHash;
    public String claveSal;
    @ColumnInfo(defaultValue = "0")
    public int intentosFallidos;
    @ColumnInfo(defaultValue = "0")
    public long bloqueadoHastaMs;
    @ColumnInfo(defaultValue = "1")
    public boolean activo = true;
    public long timestampCreacionMs;
}
