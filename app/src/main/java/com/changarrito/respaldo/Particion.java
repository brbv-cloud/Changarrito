package com.changarrito.respaldo;

import java.util.ArrayList;
import java.util.List;

/**
 * Parte una lista en pedazos de un tamano maximo. Firestore limita cada documento
 * a 1 MB, asi que las ventas (que crecen todos los dias) se guardan en varias
 * "partes" de pocas filas cada una, en vez de un solo documento gigante.
 */
public final class Particion {

    private Particion() {
    }

    public static <T> List<List<T>> partir(List<T> filas, int tamano) {
        if (tamano <= 0) throw new IllegalArgumentException("tamano debe ser > 0");
        List<List<T>> partes = new ArrayList<>();
        if (filas == null) return partes;
        for (int i = 0; i < filas.size(); i += tamano) {
            partes.add(new ArrayList<>(filas.subList(i, Math.min(filas.size(), i + tamano))));
        }
        return partes;
    }

    /** Nombre del documento de una parte: "ventas_0003". Ordenable alfabeticamente. */
    public static String idParte(String tabla, int indice) {
        return String.format(java.util.Locale.US, "%s_%04d", tabla, indice);
    }
}
