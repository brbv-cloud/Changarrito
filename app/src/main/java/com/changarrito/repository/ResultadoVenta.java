package com.changarrito.repository;

/**
 * Resultado de intentar registrar una venta: éxito, o el motivo por el que se rechazó
 * (stock insuficiente, cantidad inválida para la unidad de medida, etc.).
 */
public class ResultadoVenta {
    public final boolean exito;
    public final String error;

    private ResultadoVenta(boolean exito, String error) {
        this.exito = exito;
        this.error = error;
    }

    public static ResultadoVenta ok() {
        return new ResultadoVenta(true, null);
    }

    public static ResultadoVenta error(String mensaje) {
        return new ResultadoVenta(false, mensaje);
    }
}
