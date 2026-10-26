package com.changarrito.utils;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;

/**
 * Lo que la lista de Inventario necesita saber de un producto, en Java puro (sin
 * Android) para poder probarlo con JUnit:
 *  - cuantos dias le quedan antes de caducar, por dia de calendario (no por 24 h);
 *  - si la caducidad ya merece aviso (mismo plazo que las alertas: 7 dias);
 *  - si un producto coincide con lo que se escribe en el buscador, sin importar
 *    mayusculas ni acentos ("jamon" encuentra "Jamón").
 */
public final class EstadoProducto {

    /** Igual que AlertaRepository: con 7 dias o menos ya se avisa. */
    public static final int DIAS_AVISO_CADUCIDAD = 7;

    public enum Caducidad { SIN_FECHA, VIGENTE, PROXIMA, CADUCADO }

    private EstadoProducto() {
    }

    /**
     * Dias de calendario entre hoy y la fecha de caducidad: 0 = vence hoy, 1 = manana,
     * negativo = ya caduco. Se cuenta por dia (no por horas) para que un producto que
     * vence manana a las 00:00 no diga "vence hoy" por la noche.
     */
    public static long diasParaCaducar(long fechaCaducidadMs, long ahoraMs) {
        long dia = 24L * 60 * 60 * 1000;
        double diferencia = (inicioDelDia(fechaCaducidadMs) - inicioDelDia(ahoraMs)) / (double) dia;
        return Math.round(diferencia); // round: absorbe el cambio de horario de verano
    }

    public static Caducidad caducidad(long fechaCaducidadMs, long ahoraMs) {
        if (fechaCaducidadMs <= 0) return Caducidad.SIN_FECHA;
        long dias = diasParaCaducar(fechaCaducidadMs, ahoraMs);
        if (dias < 0) return Caducidad.CADUCADO;
        if (dias <= DIAS_AVISO_CADUCIDAD) return Caducidad.PROXIMA;
        return Caducidad.VIGENTE;
    }

    /** true si la caducidad debe llamar la atencion (proxima o ya caducado). */
    public static boolean caducidadConAviso(long fechaCaducidadMs, long ahoraMs) {
        Caducidad c = caducidad(fechaCaducidadMs, ahoraMs);
        return c == Caducidad.PROXIMA || c == Caducidad.CADUCADO;
    }

    /**
     * El buscador: cada palabra escrita debe aparecer en alguno de los campos
     * (nombre, categoria, codigo de barras...). Busqueda vacia = todo coincide.
     */
    public static boolean coincide(String busqueda, String... campos) {
        String b = normalizar(busqueda);
        if (b.isEmpty()) return true;
        StringBuilder todo = new StringBuilder();
        for (String c : campos) {
            if (c != null) todo.append(normalizar(c)).append(' ');
        }
        String texto = todo.toString();
        for (String palabra : b.split("\\s+")) {
            if (!texto.contains(palabra)) return false;
        }
        return true;
    }

    /** "  Jamón  Ñ " → "jamon n": minusculas, sin acentos y sin espacios de sobra. */
    static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static long inicioDelDia(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
