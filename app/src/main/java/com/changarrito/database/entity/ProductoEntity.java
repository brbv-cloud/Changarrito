package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "producto")
public class ProductoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public double precio;
    public int cantidadDisponible;
    public long fechaCaducidadMs;
    public int umbralStockBajo;
    public String telefonoProveedor;
    public String barcode;
    public long timestampCreacionMs;
    public long timestampActualizacionMs;
}