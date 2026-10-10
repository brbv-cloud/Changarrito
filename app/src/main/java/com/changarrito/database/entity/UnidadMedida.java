package com.changarrito.database.entity;

/**
 * Unidades de venta. v5: se quita LT (ningún giro objetivo vende por litro) y se
 * agrega M (metro) para ferretería: cable, manguera, cadena. Cada giro habilita
 * solo las que le sirven (ver ConfiguracionNegocioEntity.unidadesHabilitadas).
 */
public enum UnidadMedida {
    PZA, KG, GR, M
}
