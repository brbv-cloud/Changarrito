package com.changarrito.reportes;

import java.util.Calendar;

/** Rangos del reporte de Ganancias. Siempre terminan al final del dia de hoy. */
public enum RangoReporte {
    HOY(1), SIETE_DIAS(7), TREINTA_DIAS(30);

    public final int dias;

    RangoReporte(int dias) {
        this.dias = dias;
    }

    /**
     * [inicio, fin] en ms: desde la medianoche de hace (dias - 1) dias hasta las
     * 23:59:59.999 de hoy. Se usa Calendar para respetar cambios de horario.
     */
    public long[] calcular(long ahoraMs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ahoraMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        long fin = cal.getTimeInMillis() - 1;
        cal.add(Calendar.DAY_OF_YEAR, -dias);
        return new long[]{cal.getTimeInMillis(), fin};
    }
}
