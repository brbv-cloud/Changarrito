package com.changarrito.database.dao;

import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.database.entity.UnidadMedida;

/**
 * Resultado de la consulta "alertas activas con datos del producto" (JOIN alerta +
 * producto). No es una entidad de BD, es un POJO de proyeccion para no tener que
 * hacer una consulta extra por cada fila al mostrar la lista de alertas.
 */
public class AlertaConProducto {
    public int id;
    public int idProducto;
    public TipoAlerta tipoAlerta;
    public EstadoAlerta estado;
    public long timestampCreacionMs;

    public String nombreProducto;
    public double cantidadDisponible;
    public UnidadMedida unidadMedida;
    public long fechaCaducidadMs;
    public String telefonoProveedor;
}
