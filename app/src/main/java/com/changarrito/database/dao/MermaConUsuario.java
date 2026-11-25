package com.changarrito.database.dao;

import androidx.room.Embedded;

import com.changarrito.database.entity.MermaEntity;

/** Una merma con el nombre de quien la registro (null si fue sin turno). */
public class MermaConUsuario {
    @Embedded
    public MermaEntity merma;
    public String nombreUsuario;
}
