package com.changarrito.reportes;

/**
 * "De cada $100 que cobras": cuanto es para volver a surtir y cuanto es ganancia.
 * Es el mismo "% del precio de venta", dicho en pesos para que se entienda solo.
 */
public final class ReparticionPesos {

    private ReparticionPesos() {
    }

    /**
     * @return {repones, ganas} sobre $100. null si no hay ventas con costo.
     *         Si se vendio bajo costo, repones es mayor a 100 y ganas es negativo.
     */
    public static double[] deCada100(double ventasConCosto, double costo) {
        if (ventasConCosto <= 0 || costo <= 0) return null;
        double repones = costo / ventasConCosto * 100.0;
        double r = Math.round(repones * 100) / 100.0;
        return new double[]{r, Math.round((100.0 - r) * 100) / 100.0};
    }
}
