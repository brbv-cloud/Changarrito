package com.changarrito;

import static org.junit.Assert.assertEquals;

import com.changarrito.negocio.ControlIntentos;

import org.junit.Test;

public class ControlIntentosTest {

    @Test
    public void cuatro_fallos_no_bloquean() {
        long[] estado = {0, 0};
        for (int i = 0; i < 4; i++) estado = ControlIntentos.registrarFallo((int) estado[0], 1_000);
        assertEquals(4, estado[0]);
        assertEquals(0, estado[1]);
        assertEquals(1, ControlIntentos.intentosRestantes((int) estado[0]));
    }

    @Test
    public void quinto_fallo_bloquea_cinco_minutos_y_reinicia_el_contador() {
        long[] estado = ControlIntentos.registrarFallo(4, 1_000);
        assertEquals(0, estado[0]);
        assertEquals(1_000 + ControlIntentos.BLOQUEO_MS, estado[1]);
    }

    @Test
    public void tiempo_restante_de_bloqueo() {
        assertEquals(60_000, ControlIntentos.msRestantes(100_000, 40_000));
        assertEquals(0, ControlIntentos.msRestantes(100_000, 200_000));
        assertEquals(0, ControlIntentos.msRestantes(0, 5));
    }
}
