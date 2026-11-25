package com.changarrito.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Cuentas del momento de cobrar, en Java puro para probarlas con JUnit:
 *  - con que billetes es probable que pague el cliente (botones rapidos);
 *  - cuanto cambio dar y con que billetes y monedas darlo.
 * Todo se calcula en centavos enteros para no arrastrar errores de los double
 * (0.1 + 0.2 != 0.3).
 */
public final class Cobro {

    /** Billetes y monedas de Mexico que se usan para dar cambio, en centavos. */
    private static final long[] DENOMINACIONES = {50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50};
    /** Billetes con los que suele pagar la gente, en centavos. */
    private static final long[] BILLETES = {2000, 5000, 10000, 20000, 50000, 100000};
    private static final int MAX_SUGERENCIAS = 4;

    private Cobro() {
    }

    public static long aCentavos(double pesos) {
        return Math.round(pesos * 100);
    }

    /**
     * Montos con los que probablemente paga el cliente, de menor a mayor y siempre
     * mayores al total: redondeos a 10, 20, 50 y 100, y el siguiente billete.
     * Ej. total $128.50 → $130, $140, $150, $200.
     */
    public static List<Double> sugerenciasPago(double total) {
        long t = aCentavos(total);
        List<Double> out = new ArrayList<>();
        if (t <= 0) return out;
        TreeSet<Long> montos = new TreeSet<>();
        for (long paso : new long[]{1000, 2000, 5000, 10000}) {
            montos.add(redondearArriba(t, paso));
        }
        for (long b : BILLETES) {
            if (b >= t) {
                montos.add(b);
                break;
            }
        }
        for (Long m : montos) {
            if (m > t && out.size() < MAX_SUGERENCIAS) out.add(m / 100.0);
        }
        return out;
    }

    private static long redondearArriba(long valor, long paso) {
        return ((valor + paso - 1) / paso) * paso;
    }

    /** Una denominacion y cuantas piezas de ella. */
    public static final class Pieza {
        public final long centavos;
        public final int cantidad;

        Pieza(long centavos, int cantidad) {
            this.centavos = centavos;
            this.cantidad = cantidad;
        }
    }

    /**
     * Como dar el cambio con la menor cantidad de billetes y monedas.
     * Si sobran centavos que no llegan a 50¢ se regresan aparte en {@link #centavosSueltos}.
     */
    public static List<Pieza> desgloseCambio(double cambio) {
        long resto = aCentavos(cambio);
        List<Pieza> piezas = new ArrayList<>();
        for (long d : DENOMINACIONES) {
            if (resto >= d) {
                int n = (int) (resto / d);
                piezas.add(new Pieza(d, n));
                resto -= n * d;
            }
        }
        return piezas;
    }

    public static long centavosSueltos(double cambio) {
        return aCentavos(cambio) % 50;
    }

    /** "$50 + $20 + 2 × $1 + 50¢" (y "+ 30¢" si quedan centavos sueltos). Vacio si no hay cambio. */
    public static String textoDesglose(double cambio) {
        StringBuilder sb = new StringBuilder();
        for (Pieza p : desgloseCambio(cambio)) {
            if (sb.length() > 0) sb.append(" + ");
            if (p.cantidad > 1) sb.append(p.cantidad).append(" × ");
            sb.append(nombre(p.centavos));
        }
        long sueltos = centavosSueltos(cambio);
        if (sueltos > 0) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(sueltos).append('¢');
        }
        return sb.toString();
    }

    private static String nombre(long centavos) {
        if (centavos < 100) return centavos + "¢";
        return String.format(Locale.US, "$%d", centavos / 100);
    }
}
