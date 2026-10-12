package com.changarrito.database.dao;

/** Ventas agrupadas por la persona que tenia el turno. idUsuario null = sin turno o perfil borrado. */
public class VentasPorUsuario {
    public Integer idUsuario;
    public String nombre;
    public int transacciones;
    public double ventas;
}
