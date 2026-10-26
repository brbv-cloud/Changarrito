package com.changarrito.database.dao;

import androidx.room.Embedded;

import com.changarrito.database.entity.CierreCajaEntity;

/** Un cierre de caja con el nombre de quien lo hizo (null si fue sin turno). */
public class CierreConUsuario {
    @Embedded
    public CierreCajaEntity cierre;
    public String nombreUsuario;
}
