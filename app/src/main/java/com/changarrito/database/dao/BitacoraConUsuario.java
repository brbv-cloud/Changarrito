package com.changarrito.database.dao;

import androidx.room.Embedded;

import com.changarrito.database.entity.BitacoraEntity;

/** Entrada de bitácora + nombre de quien hizo el cambio (LEFT JOIN con usuario). */
public class BitacoraConUsuario {
    @Embedded
    public BitacoraEntity entrada;

    public String nombreUsuario;
}
