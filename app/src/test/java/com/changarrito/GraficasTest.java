package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.changarrito.database.dao.GananciaDia;
import com.changarrito.reportes.DiaReporte;
import com.changarrito.reportes.EscalaGrafica;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.reportes.ReparticionPesos;
import com.changarrito.reportes.SerieDiaria;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class GraficasTest {

    private static final double D = 0.001;

    private static GananciaDia dia(String fecha, double ventas, double conCosto, double costo) {
        GananciaDia g = new GananciaDia();
        g.fecha = fecha;
        g.ventas = ventas;
        g.ventasConCosto = conCosto;
        g.costo = costo;
        return g;
    }

    private static long hoyA(int hora) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 25, hora, 0, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void completar_rellena_dias_sin_ventas_con_cero() {
        long[] r = RangoReporte.SIETE_DIAS.calcular(hoyA(20));
        List<DiaReporte> serie = SerieDiaria.completar(
                Arrays.asList(dia("2026-10-20", 100, 100, 60), dia("2026-10-25", 50, 30, 20)), r[0], r[1]);
        assertEquals(7, serie.size());
        assertEquals(0, serie.get(0).ventas, D);          // 19 oct sin ventas
        assertEquals(100, serie.get(1).ventas, D);        // 20 oct
        assertEquals(50, serie.get(6).ventas, D);         // 25 oct (hoy)
        Calendar primero = Calendar.getInstance();
        primero.setTimeInMillis(serie.get(0).diaMs);
        assertEquals(19, primero.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void completar_hoy_da_un_solo_dia_y_nulo_no_truena() {
        long[] r = RangoReporte.HOY.calcular(hoyA(9));
        assertEquals(1, SerieDiaria.completar(null, r[0], r[1]).size());
        assertEquals(30, SerieDiaria.completar(Collections.emptyList(),
                RangoReporte.TREINTA_DIAS.calcular(hoyA(9))[0], r[1]).size());
    }

    @Test
    public void dia_se_parte_en_repones_ganas_y_sin_costo() {
        DiaReporte d = new DiaReporte();
        d.ventas = 150;          // 50 de productos sin costo
        d.ventasConCosto = 100;
        d.costo = 60;
        assertEquals(60, d.repones(), D);
        assertEquals(40, d.ganas(), D);
        assertEquals(50, d.sinCosto(), D);
        assertEquals(150, d.repones() + d.ganas() + d.sinCosto(), D);
        assertFalse(d.huboPerdida());
    }

    @Test
    public void dia_con_perdida_no_dibuja_ganancia_negativa() {
        DiaReporte d = new DiaReporte();
        d.ventas = 20;
        d.ventasConCosto = 20;
        d.costo = 25;
        assertEquals(20, d.repones(), D);   // la barra no pasa de lo vendido
        assertEquals(0, d.ganas(), D);
        assertTrue(d.huboPerdida());
        assertEquals(-5, d.ganancia(), D);
    }

    @Test
    public void total_suma_toda_la_serie() {
        long[] r = RangoReporte.SIETE_DIAS.calcular(hoyA(12));
        List<DiaReporte> serie = SerieDiaria.completar(
                Arrays.asList(dia("2026-10-21", 100, 100, 60), dia("2026-10-22", 50, 50, 30)), r[0], r[1]);
        DiaReporte t = SerieDiaria.total(serie);
        assertEquals(150, t.ventas, D);
        assertEquals(60, t.ganancia(), D);
    }

    @Test
    public void de_cada_100_ejemplo_del_perfume() {
        // Vendiste 2600, te costo 2000: de cada $100, $76.92 repones y $23.08 ganas
        double[] r = ReparticionPesos.deCada100(2600, 2000);
        assertEquals(76.92, r[0], D);
        assertEquals(23.08, r[1], D);
        assertEquals(100, r[0] + r[1], D);
    }

    @Test
    public void de_cada_100_sin_costo_es_nulo_y_con_perdida_es_negativo() {
        assertNull(ReparticionPesos.deCada100(100, 0));
        assertNull(ReparticionPesos.deCada100(0, 0));
        double[] perdida = ReparticionPesos.deCada100(20, 25);
        assertEquals(125, perdida[0], D);
        assertEquals(-25, perdida[1], D);
    }

    @Test
    public void techo_redondea_a_valores_bonitos() {
        assertEquals(100, EscalaGrafica.techo(0), D);
        assertEquals(1000, EscalaGrafica.techo(870), D);
        assertEquals(2500, EscalaGrafica.techo(2100), D);
        assertEquals(5000, EscalaGrafica.techo(2600), D);
        assertEquals(200, EscalaGrafica.techo(200), D);
        assertEquals(10, EscalaGrafica.techo(7.5), D);
    }

    @Test
    public void dinero_corto_para_etiquetas() {
        assertEquals("$850", EscalaGrafica.dineroCorto(850));
        assertEquals("$2.6 mil", EscalaGrafica.dineroCorto(2600));
        assertEquals("$2 mil", EscalaGrafica.dineroCorto(2000));
        assertEquals("$15 mil", EscalaGrafica.dineroCorto(15200));
        assertEquals("$1.3 M", EscalaGrafica.dineroCorto(1_300_000));
        assertEquals("-$40", EscalaGrafica.dineroCorto(-40));
    }

    @Test
    public void paso_de_etiquetas_evita_que_se_encimen() {
        assertEquals(1, EscalaGrafica.pasoEtiquetas(7, 8));
        assertEquals(4, EscalaGrafica.pasoEtiquetas(30, 8));
    }
}
