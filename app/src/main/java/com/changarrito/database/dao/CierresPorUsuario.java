package com.changarrito.database.dao;

/** Cierres de caja agrupados por quien los hizo. faltante es negativo o 0; sobrante positivo o 0. */
public class CierresPorUsuario {
    public Integer idUsuario;
    public String nombre;
    public int cierres;
    public double faltante;
    public double sobrante;
}
