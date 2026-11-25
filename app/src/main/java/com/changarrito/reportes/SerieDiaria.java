package com.changarrito.reportes;

import com.changarrito.database.dao.GananciaDia;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Convierte lo que regresa la base de datos en una serie con TODOS los dias del rango. */
public final class SerieDiaria {

    private SerieDiaria() {
    }

    public static List<DiaReporte> completar(List<GananciaDia> datos, long inicioMs, long finMs) {
        Map<String, GananciaDia> porFecha = new HashMap<>();
        if (datos != null) {
            for (GananciaDia d : datos) {
                if (d != null && d.fecha != null) porFecha.put(d.fecha, d);
            }
        }
        SimpleDateFormat clave = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(inicioMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        List<DiaReporte> serie = new ArrayList<>();
        // Tope de seguridad: un rango mal calculado nunca genera miles de barras.
        while (cal.getTimeInMillis() <= finMs && serie.size() < 400) {
            DiaReporte dia = new DiaReporte();
            dia.diaMs = cal.getTimeInMillis();
            GananciaDia g = porFecha.get(clave.format(new Date(dia.diaMs)));
            if (g != null) {
                dia.ventas = g.ventas;
                dia.ventasConCosto = g.ventasConCosto;
                dia.costo = g.costo;
            }
            serie.add(dia);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return serie;
    }

    /** Totales de toda la serie, con la misma forma que un dia. */
    public static DiaReporte total(List<DiaReporte> serie) {
        DiaReporte t = new DiaReporte();
        if (serie == null) return t;
        for (DiaReporte d : serie) {
            t.ventas += d.ventas;
            t.ventasConCosto += d.ventasConCosto;
            t.costo += d.costo;
        }
        return t;
    }
}
