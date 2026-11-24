package com.changarrito.database.dao;

import androidx.room.Embedded;

import com.changarrito.database.entity.GastoEntity;

/** Un gasto con el nombre de quien lo registro (null si fue sin turno). */
public class GastoConUsuario {
    @Embedded
    public GastoEntity gasto;
    public String nombreUsuario;
}
