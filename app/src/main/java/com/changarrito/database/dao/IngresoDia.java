package com.changarrito.database.dao;

/**
 * Resultado de "ingresos agrupados por dia" (para la grafica de los ultimos N
 * dias). No es una entidad de BD, es un POJO de proyeccion.
 */
public class IngresoDia {
    public String fecha; // "yyyy-MM-dd", zona horaria local del dispositivo
    public double total;
}
