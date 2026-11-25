package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorUsuario;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.RangoReporte;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class GananciasTest {

    private static final double D = 0.001;

    private static ResumenGanancia resumen(double ventasConCosto, double costo) {
        ResumenGanancia r = new ResumenGanancia();
        r.ventas = ventasConCosto;
        r.ventasConCosto = ventasConCosto;
        r.costo = costo;
        return r;
    }

    private static GananciaProducto producto(String nombre, double ventasConCosto, double costo) {
        GananciaProducto p = new GananciaProducto();
        p.nombre = nombre;
        p.ventas = ventasConCosto;
        p.ventasConCosto = ventasConCosto;
        p.costo = costo;
        return p;
    }

    @Test
    public void resumen_ejemploAcordado_25a37_50() {
        // Compra en $25, vende en $37.50: gana $12.50 = 50% sobre costo = 33.3% del precio
        ResumenGanancia r = resumen(37.50, 25.0);
        assertEquals(12.50, CalculoGanancias.ganancia(r), D);
        assertEquals(50.0, CalculoGanancias.pctSobreCosto(r), D);
        assertEquals(33.333, CalculoGanancias.pctDelPrecio(r), D);
    }

    @Test
    public void resumen_ventasSinCosto_noInflanGanancia() {
        ResumenGanancia r = resumen(100, 60);
        r.ventas = 150;          // $50 vendidos de productos sin costo
        r.importeSinCosto = 50;
        r.ventasSinCosto = 2;
        assertEquals(40.0, CalculoGanancias.ganancia(r), D);
    }

    @Test
    public void resumen_nulo_daCero() {
        assertEquals(0, CalculoGanancias.ganancia(null), D);
        assertEquals(0, CalculoGanancias.pctSobreCosto((ResumenGanancia) null), D);
    }

    @Test
    public void resumen_vendidoBajoCosto_gananciaNegativa() {
        ResumenGanancia r = resumen(20, 25);
        assertEquals(-5.0, CalculoGanancias.ganancia(r), D);
        assertTrue(CalculoGanancias.pctSobreCosto(r) < 0);
    }

    @Test
    public void menosRentables_soloAmarillosYRojos_ordenadosDelPeor() {
        List<GananciaProducto> lista = Arrays.asList(
                producto("Verde", 150, 100),     // 50%
                producto("Amarillo", 105, 100),  // 5%
                producto("Rojo", 90, 100),       // -10%
                producto("SinCosto", 50, 0));
        List<GananciaProducto> bajos = CalculoGanancias.menosRentables(lista, 5);
        assertEquals(2, bajos.size());
        assertEquals("Rojo", bajos.get(0).nombre);
        assertEquals("Amarillo", bajos.get(1).nombre);
    }

    @Test
    public void menosRentables_respetaMaximo() {
        List<GananciaProducto> lista = Arrays.asList(
                producto("A", 101, 100), producto("B", 102, 100), producto("C", 103, 100));
        assertEquals(2, CalculoGanancias.menosRentables(lista, 2).size());
        assertEquals(0, CalculoGanancias.menosRentables(null, 5).size());
    }

    @Test
    public void combinarPersonas_juntaVentasYCierres() {
        VentasPorUsuario ana = new VentasPorUsuario();
        ana.idUsuario = 2; ana.nombre = "Ana"; ana.transacciones = 3; ana.ventas = 300;
        VentasPorUsuario dueno = new VentasPorUsuario();
        dueno.idUsuario = 1; dueno.nombre = "Bruno"; dueno.transacciones = 5; dueno.ventas = 500;
        CierresPorUsuario cAna = new CierresPorUsuario();
        cAna.idUsuario = 2; cAna.nombre = "Ana"; cAna.cierres = 1; cAna.faltante = -15;
        CierresPorUsuario cLuis = new CierresPorUsuario();
        cLuis.idUsuario = 3; cLuis.nombre = "Luis"; cLuis.cierres = 1; cLuis.sobrante = 5;

        List<CalculoGanancias.FilaPersona> filas =
                CalculoGanancias.combinarPersonas(Arrays.asList(ana, dueno), Arrays.asList(cAna, cLuis));

        assertEquals(3, filas.size());
        assertEquals("Bruno", filas.get(0).nombre);           // el que mas vendio primero
        assertEquals("Ana", filas.get(1).nombre);
        assertEquals(-15, filas.get(1).faltante, D);
        assertEquals("Luis", filas.get(2).nombre);            // solo hizo cierre, sin ventas
        assertEquals(0, filas.get(2).ventas, D);
    }

    @Test
    public void combinarPersonas_sinTurnoSeAgrupaEnUnaFila() {
        VentasPorUsuario sinTurno = new VentasPorUsuario();
        sinTurno.idUsuario = null; sinTurno.transacciones = 2; sinTurno.ventas = 40;
        CierresPorUsuario cierreSinTurno = new CierresPorUsuario();
        cierreSinTurno.idUsuario = null; cierreSinTurno.cierres = 1;
        List<CalculoGanancias.FilaPersona> filas = CalculoGanancias.combinarPersonas(
                Arrays.asList(sinTurno), Arrays.asList(cierreSinTurno));
        assertEquals(1, filas.size());
        assertNull(filas.get(0).idUsuario);
        assertEquals(1, filas.get(0).cierres);
    }

    @Test
    public void rango_hoyEmpiezaAMedianocheYTerminaAlFinalDelDia() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 11, 15, 30, 0);
        long[] r = RangoReporte.HOY.calcular(c.getTimeInMillis());
        Calendar ini = Calendar.getInstance();
        ini.setTimeInMillis(r[0]);
        assertEquals(11, ini.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, ini.get(Calendar.HOUR_OF_DAY));
        assertEquals(24L * 60 * 60 * 1000 - 1, r[1] - r[0]);
    }

    @Test
    public void rango_sieteDiasIncluyeHoyYSeisAtras() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 11, 9, 0, 0);
        long[] r = RangoReporte.SIETE_DIAS.calcular(c.getTimeInMillis());
        Calendar ini = Calendar.getInstance();
        ini.setTimeInMillis(r[0]);
        assertEquals(5, ini.get(Calendar.DAY_OF_MONTH));
        assertTrue(r[1] > c.getTimeInMillis());
    }

    @Test
    public void formato_dineroConMilesYNegativos() {
        assertEquals("$1,234.50", CalculoGanancias.dinero(1234.5));
        assertEquals("-$5.00", CalculoGanancias.dinero(-5));
        assertEquals("$0.00", CalculoGanancias.dinero(-0.001));
        assertEquals("50.0%", CalculoGanancias.porcentaje(50));
    }
}
