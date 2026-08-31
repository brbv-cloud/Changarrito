package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "producto")
public class ProductoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public double precio;
    public double cantidadDisponible;
    public UnidadMedida unidadMedida;
    public long fechaCaducidadMs;
    public double umbralStockBajo;
    public String telefonoProveedor;
    public String barcode;
    public long timestampCreacionMs;
    public long timestampActualizacionMs;
}