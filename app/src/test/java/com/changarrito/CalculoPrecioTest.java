package com.changarrito;

import static org.junit.Assert.assertEquals;

import com.changarrito.utils.CalculoPrecio;

import org.junit.Test;

/** Fórmulas acordadas: el % que se captura es SOBRE EL COSTO. */
public class CalculoPrecioTest {

    private static final double D = 0.001;

    @Test
    public void precio_sugerido_con_50_por_ciento_sobre_costo() {
        // El ejemplo que acordamos: costo $25 y 50% → $37.50 (no $50).
        assertEquals(37.50, CalculoPrecio.precioSugerido(25, 50, false), D);
    }

    @Test
    public void precio_sugerido_con_30_por_ciento() {
        assertEquals(32.50, CalculoPrecio.precioSugerido(25, 30, false), D);
    }

    @Test
    public void redondeo_sube_al_siguiente_medio_peso() {
        // 10 × 1.23 = 12.30 → 12.50
        assertEquals(12.50, CalculoPrecio.precioSugerido(10, 23, true), D);
        // 25 × 1.30 = 32.50 exacto: no debe subir a 33.00 por errores de redondeo.
        assertEquals(32.50, CalculoPrecio.precioSugerido(25, 30, true), D);
    }

    @Test
    public void margen_sobre_costo_y_sobre_precio() {
        assertEquals(50.0, CalculoPrecio.margenSobreCosto(25, 37.5), D);
        assertEquals(33.333, CalculoPrecio.margenSobrePrecio(25, 37.5), D);
    }

    @Test
    public void sin_costo_o_sin_precio_no_divide_entre_cero() {
        assertEquals(0, CalculoPrecio.margenSobreCosto(0, 10), D);
        assertEquals(0, CalculoPrecio.margenSobrePrecio(5, 0), D);
    }

    @Test
    public void semaforo() {
        assertEquals(CalculoPrecio.Semaforo.ROJO, CalculoPrecio.semaforo(25, 20));
        assertEquals(CalculoPrecio.Semaforo.AMARILLO, CalculoPrecio.semaforo(25, 26));
        assertEquals(CalculoPrecio.Semaforo.VERDE, CalculoPrecio.semaforo(25, 30));
    }

    @Test
    public void ganancia_por_unidad() {
        assertEquals(12.50, CalculoPrecio.gananciaPorUnidad(25, 37.5), D);
        assertEquals(-5.00, CalculoPrecio.gananciaPorUnidad(25, 20), D);
    }
}
