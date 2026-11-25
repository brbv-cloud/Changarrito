package com.changarrito.database.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tipo de negocio que elige el dueño después de iniciar sesión. Cada giro trae su
 * configuración por defecto (unidades, caducidad, catálogo precargado opcional);
 * el dueño la puede ajustar después en ConfiguracionNegocioEntity.
 */
public enum GiroNegocio {
    ABARROTES("Abarrotes", Arrays.asList(UnidadMedida.PZA, UnidadMedida.KG, UnidadMedida.GR),
            ModoCaducidad.OPCIONAL, "catalogo_abarrotes.json"),
    PAPELERIA("Papelería", Collections.singletonList(UnidadMedida.PZA),
            ModoCaducidad.OCULTA, "catalogo_papeleria.json"),
    FERRETERIA("Ferretería", Arrays.asList(UnidadMedida.PZA, UnidadMedida.KG, UnidadMedida.M),
            ModoCaducidad.OCULTA, "catalogo_ferreteria.json"),
    FARMACIA("Farmacia", Collections.singletonList(UnidadMedida.PZA),
            ModoCaducidad.OBLIGATORIA, "catalogo_farmacia.json"),
    INDEPENDIENTE("Negocio independiente", Arrays.asList(UnidadMedida.values()),
            ModoCaducidad.OPCIONAL, null);

    public final String nombreVisible;
    public final List<UnidadMedida> unidadesPorDefecto;
    public final ModoCaducidad caducidadPorDefecto;
    /** Archivo en assets/ con el catálogo sugerido; null = el negocio arma su propio inventario. */
    public final String archivoCatalogo;

    GiroNegocio(String nombreVisible, List<UnidadMedida> unidadesPorDefecto,
                ModoCaducidad caducidadPorDefecto, String archivoCatalogo) {
        this.nombreVisible = nombreVisible;
        this.unidadesPorDefecto = unidadesPorDefecto;
        this.caducidadPorDefecto = caducidadPorDefecto;
        this.archivoCatalogo = archivoCatalogo;
    }
}
