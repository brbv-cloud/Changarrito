package com.changarrito.database.dao;

/** Ventas agrupadas por hora del dia (0 a 23, hora local). */
public class VentasPorHora {
    public int hora;
    /** Compras distintas en esa hora. */
    public int tickets;
    public double ventas;
}
