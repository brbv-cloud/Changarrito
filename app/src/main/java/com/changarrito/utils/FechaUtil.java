package com.changarrito.utils;

import java.util.Calendar;

/**
 * Calcula el rango [inicio, fin] de un dia en milisegundos, en la zona horaria
 * local del dispositivo. Se usa tanto en ReportesRepository (ingresos de hoy)
 * como en CierreCajaRepository (efectivo del sistema del dia que se cierra).
 */
public class FechaUtil {

    private FechaUtil() {
    }

    /** Medianoche local del dia que contiene timestampMs. */
    public static long inicioDelDia(long timestampMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** [inicio, fin] del dia que contiene timestampMs (23:59:59.999 como fin). */
    public static long[] rangoDelDia(long timestampMs) {
        long inicio = inicioDelDia(timestampMs);
        long fin = inicio + (24L * 60 * 60 * 1000) - 1;
        return new long[]{inicio, fin};
    }
}
