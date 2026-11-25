package com.changarrito.database.entity;

/** Por que se perdio mercancia que no se vendio. */
public enum MotivoMerma {
    DANADO("Dañado"),
    CADUCADO("Caducado"),
    ROBO("Robo o faltante"),
    OTRO("Otro");

    public final String nombreVisible;

    MotivoMerma(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /** Nombre para mostrar a partir del texto guardado; tolera valores desconocidos. */
    public static String nombre(String guardado) {
        if (guardado == null) return OTRO.nombreVisible;
        for (MotivoMerma m : values()) {
            if (m.name().equals(guardado)) return m.nombreVisible;
        }
        return guardado;
    }
}
