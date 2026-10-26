package com.changarrito.reportes;

/**
 * Un dia de la grafica / tabla, ya con los dias sin ventas rellenados en cero.
 * Se separa en tres pedazos que suman lo vendido:
 *   repones  = lo que costo la mercancia vendida (hay que volver a surtir)
 *   ganas    = lo que te quedo (puede ser negativo si se vendio bajo costo)
 *   sinCosto = ventas de productos sin costo capturado (no se sabe cuanto dejaron)
 */
public class DiaReporte {
    public long diaMs;
    public double ventas;
    public double ventasConCosto;
    public double costo;

    public double ganancia() {
        return ventasConCosto - costo;
    }

    /** Parte de la barra que es costo (nunca mas que lo vendido con costo). */
    public double repones() {
        return Math.max(0, Math.min(costo, ventasConCosto));
    }

    /** Parte de la barra que es ganancia (0 si se perdio). */
    public double ganas() {
        return Math.max(0, ganancia());
    }

    public double sinCosto() {
        return Math.max(0, ventas - ventasConCosto);
    }

    public boolean huboPerdida() {
        return ventasConCosto > 0 && ganancia() < -0.005;
    }
}
