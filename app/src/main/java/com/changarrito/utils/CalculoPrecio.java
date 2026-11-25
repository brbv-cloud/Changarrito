package com.changarrito.utils;

/**
 * Fórmulas de precio y ganancia. Clase pura (sin Android) para probarla con JUnit.
 *
 * Convención acordada: el % que captura el usuario es SOBRE EL COSTO.
 *   precio sugerido = costo × (1 + % / 100)
 *   ej. costo $25 con 50% → $37.50
 * En los reportes se muestra además el % sobre el precio de venta:
 *   % del precio = (precio − costo) / precio × 100
 *   ej. $25 → $37.50 deja 33.3% del precio de venta como ganancia.
 */
public final class CalculoPrecio {

    /** Umbral del semáforo: debajo de este % sobre el costo, el producto "casi no deja". */
    public static final double MARGEN_BAJO = 10.0;

    public enum Semaforo { ROJO, AMARILLO, VERDE }

    private CalculoPrecio() {
    }

    /** Precio sugerido, opcionalmente redondeado HACIA ARRIBA al siguiente $0.50. */
    public static double precioSugerido(double costo, double margenSobreCosto, boolean redondearMedioPeso) {
        double precio = costo * (1 + margenSobreCosto / 100.0);
        if (redondearMedioPeso) {
            // Se redondea primero a centavos para que 32.5000000001 no suba a 33.00.
            double centavos = Math.round(precio * 100) / 100.0;
            precio = Math.ceil(centavos * 2) / 2.0;
        }
        return redondearCentavos(precio);
    }

    /** % de ganancia sobre el costo. Con costo 0 no hay base para calcularlo: devuelve 0. */
    public static double margenSobreCosto(double costo, double precio) {
        if (costo <= 0) return 0;
        return (precio - costo) / costo * 100.0;
    }

    /** % del precio de venta que es ganancia. Con precio 0 devuelve 0. */
    public static double margenSobrePrecio(double costo, double precio) {
        if (precio <= 0) return 0;
        return (precio - costo) / precio * 100.0;
    }

    public static double gananciaPorUnidad(double costo, double precio) {
        return redondearCentavos(precio - costo);
    }

    /** Rojo: se vende por debajo del costo. Amarillo: gana menos de MARGEN_BAJO. Verde: el resto. */
    public static Semaforo semaforo(double costo, double precio) {
        if (precio < costo) return Semaforo.ROJO;
        if (margenSobreCosto(costo, precio) < MARGEN_BAJO) return Semaforo.AMARILLO;
        return Semaforo.VERDE;
    }

    public static double redondearCentavos(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
