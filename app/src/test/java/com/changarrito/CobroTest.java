package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.changarrito.utils.Cobro;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CobroTest {

    @Test
    public void sugerencias_redondeosYSiguienteBillete() {
        assertEquals(Arrays.asList(130.0, 140.0, 150.0, 200.0), Cobro.sugerenciasPago(128.50));
    }

    @Test
    public void sugerencias_totalChico() {
        // $17 → $20 (redondeos a 10 y 20 coinciden), $50, $100
        assertEquals(Arrays.asList(20.0, 50.0, 100.0), Cobro.sugerenciasPago(17));
    }

    @Test
    public void sugerencias_totalRedondoNoSeRepite() {
        // $100 exacto: el boton "Exacto" ya cubre los $100, no se sugiere otra vez.
        List<Double> s = Cobro.sugerenciasPago(100);
        assertTrue(s.isEmpty() || s.get(0) > 100);
    }

    @Test
    public void sugerencias_vacioSinTotal() {
        assertTrue(Cobro.sugerenciasPago(0).isEmpty());
    }

    @Test
    public void desglose_billetesYMonedas() {
        assertEquals("$50 + $20 + $1 + 50¢", Cobro.textoDesglose(71.50));
        assertEquals("$200 + $100 + 2 × $20", Cobro.textoDesglose(340));
    }

    @Test
    public void desglose_centavosSueltos() {
        // 0.1 + 0.2 en double no da 0.3 exacto: en centavos si.
        assertEquals("$2 + 30¢", Cobro.textoDesglose(2.0 + 0.1 + 0.2));
    }

    @Test
    public void desglose_sinCambio() {
        assertEquals("", Cobro.textoDesglose(0));
    }
}
