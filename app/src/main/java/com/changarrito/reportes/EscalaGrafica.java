package com.changarrito.reportes;

import java.util.Locale;

/** Utilidades para que las graficas tengan ejes "redondos" y etiquetas cortas. */
public final class EscalaGrafica {

    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private EscalaGrafica() {
    }

    /** Siguiente valor "bonito" (1, 2, 2.5, 5 × 10^n) mayor o igual a max. */
    public static double techo(double max) {
        if (max <= 0) return 100;
        double potencia = Math.pow(10, Math.floor(Math.log10(max)));
        double[] pasos = {1, 2, 2.5, 5, 10};
        for (double p : pasos) {
            double candidato = p * potencia;
            if (candidato >= max - 1e-9) return candidato;
        }
        return 10 * potencia;
    }

    /** "$850", "$1.2 mil", "$15 mil", "$1.3 M" — para encima de las barras. */
    public static String dineroCorto(double valor) {
        double a = Math.abs(valor);
        String signo = valor < -0.004 ? "-" : "";
        if (a < 1000) return signo + String.format(MX, "$%.0f", a);
        if (a < 10_000) return signo + String.format(MX, "$%.1f mil", a / 1000).replace(".0 mil", " mil");
        if (a < 1_000_000) return signo + String.format(MX, "$%.0f mil", a / 1000);
        return signo + String.format(MX, "$%.1f M", a / 1_000_000).replace(".0 M", " M");
    }

    /** Cada cuantas barras poner la etiqueta del dia para que no se encimen. */
    public static int pasoEtiquetas(int barras, int maximoEtiquetas) {
        if (barras <= maximoEtiquetas) return 1;
        return (int) Math.ceil(barras / (double) maximoEtiquetas);
    }
}
