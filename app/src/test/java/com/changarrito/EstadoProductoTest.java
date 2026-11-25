package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.changarrito.utils.EstadoProducto;
import com.changarrito.utils.EstadoProducto.Caducidad;

import org.junit.Test;

import java.util.Calendar;

public class EstadoProductoTest {

    private static long fecha(int anio, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, hora, minuto);
        return c.getTimeInMillis();
    }

    private final long hoyNoche = fecha(2026, 10, 25, 23, 30);

    @Test
    public void venceManana_aunqueFalteMenosDeUnDia() {
        assertEquals(1, EstadoProducto.diasParaCaducar(fecha(2026, 10, 26, 0, 0), hoyNoche));
    }

    @Test
    public void venceHoy_esCero() {
        assertEquals(0, EstadoProducto.diasParaCaducar(fecha(2026, 10, 25, 8, 0), hoyNoche));
        assertEquals(Caducidad.PROXIMA, EstadoProducto.caducidad(fecha(2026, 10, 25, 8, 0), hoyNoche));
    }

    @Test
    public void ayer_yaCaduco() {
        assertEquals(-1, EstadoProducto.diasParaCaducar(fecha(2026, 10, 24, 12, 0), hoyNoche));
        assertEquals(Caducidad.CADUCADO, EstadoProducto.caducidad(fecha(2026, 10, 24, 12, 0), hoyNoche));
    }

    @Test
    public void limiteDeSieteDias() {
        assertEquals(Caducidad.PROXIMA, EstadoProducto.caducidad(fecha(2026, 11, 1, 12, 0), hoyNoche));
        assertEquals(Caducidad.VIGENTE, EstadoProducto.caducidad(fecha(2026, 11, 2, 12, 0), hoyNoche));
    }

    @Test
    public void sinFecha() {
        assertEquals(Caducidad.SIN_FECHA, EstadoProducto.caducidad(0, hoyNoche));
        assertFalse(EstadoProducto.caducidadConAviso(0, hoyNoche));
    }

    @Test
    public void buscador_ignoraAcentosYMayusculas() {
        assertTrue(EstadoProducto.coincide("jamon", "Jamón de pavo", "Salchichonería", null));
        assertTrue(EstadoProducto.coincide("LECHE", "Leche Alpura 1L", null, null));
        assertTrue(EstadoProducto.coincide("salchich", "Jamón", "Salchichonería", null));
    }

    @Test
    public void buscador_cadaPalabraDebeEstar() {
        assertTrue(EstadoProducto.coincide("leche 1l", "Leche Alpura 1L", "Lácteos", null));
        assertFalse(EstadoProducto.coincide("leche deslactosada", "Leche Alpura 1L", "Lácteos", null));
    }

    @Test
    public void buscador_porCodigoYVacio() {
        assertTrue(EstadoProducto.coincide("7501055", "Coca-Cola 600ml", "Refrescos", "7501055300846"));
        assertTrue(EstadoProducto.coincide("   ", "Lo que sea", null, null));
        assertTrue(EstadoProducto.coincide(null, "Lo que sea", null, null));
    }
}
