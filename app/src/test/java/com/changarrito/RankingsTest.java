package com.changarrito;

import static org.junit.Assert.assertEquals;

import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.reportes.Rankings;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankingsTest {

    private static final double D = 0.001;

    private static GananciaProducto prod(String n, double cantidad, double ventas, double costo) {
        GananciaProducto p = new GananciaProducto();
        p.nombre = n;
        p.cantidad = cantidad;
        p.ventas = ventas;
        p.ventasConCosto = costo > 0 ? ventas : 0;
        p.costo = costo;
        return p;
    }

    private static VentasPorHora hora(int h, int tickets) {
        VentasPorHora v = new VentasPorHora();
        v.hora = h;
        v.tickets = tickets;
        return v;
    }

    private static VentasPorCategoria cat(String n, double ventas) {
        VentasPorCategoria c = new VentasPorCategoria();
        c.categoria = n;
        c.ventas = ventas;
        return c;
    }

    @Test
    public void ticket_promedio_divide_entre_compras_no_entre_productos() {
        ResumenGanancia r = new ResumenGanancia();
        r.ventas = 1300;
        r.transacciones = 20;  // 20 lineas
        r.tickets = 7;         // pero solo 7 clientes
        assertEquals(185.714, Rankings.ticketPromedio(r), D);
        assertEquals(0, Rankings.ticketPromedio(new ResumenGanancia()), D);
        assertEquals(0, Rankings.ticketPromedio(null), D);
    }

    @Test
    public void mas_vendidos_por_cantidad_y_mas_rentables_por_ganancia() {
        List<GananciaProducto> lista = Arrays.asList(
                prod("Chicle", 200, 200, 120),     // mucho volumen, gana 80
                prod("Perfume", 2, 2600, 2000),    // poco volumen, gana 600
                prod("Refresco", 50, 850, 600),    // gana 250
                prod("Sin costo", 300, 300, 0));
        List<GananciaProducto> vendidos = Rankings.masVendidos(lista, 2);
        assertEquals("Sin costo", vendidos.get(0).nombre);
        assertEquals("Chicle", vendidos.get(1).nombre);

        List<GananciaProducto> rentables = Rankings.masRentables(lista, 10);
        assertEquals(3, rentables.size());            // el que no tiene costo no entra
        assertEquals("Perfume", rentables.get(0).nombre);
        assertEquals("Refresco", rentables.get(1).nombre);
    }

    @Test
    public void top_con_lista_vacia_o_nula() {
        assertEquals(0, Rankings.masVendidos(null, 10).size());
        assertEquals(0, Rankings.masRentables(new ArrayList<>(), 10).size());
    }

    @Test
    public void hora_pico_y_horas_visibles() {
        int[] porHora = Rankings.ticketsPorHora(Arrays.asList(hora(9, 3), hora(18, 12), hora(19, 12), hora(23, 1)));
        assertEquals(12, porHora[18]);
        assertEquals(18, Rankings.horaPico(porHora));          // empate: gana la mas temprana
        assertEquals(-1, Rankings.horaPico(new int[24]));
        int[] visibles = Rankings.horasVisibles(porHora);
        assertEquals(7, visibles[0]);
        assertEquals(23, visibles[1]);                          // se amplia por la venta de las 11 pm
    }

    @Test
    public void etiquetas_de_hora() {
        assertEquals("12 am", Rankings.etiquetaHora(0));
        assertEquals("7 am", Rankings.etiquetaHora(7));
        assertEquals("12 pm", Rankings.etiquetaHora(12));
        assertEquals("6 pm", Rankings.etiquetaHora(18));
    }

    @Test
    public void categorias_junta_las_chicas_en_otras() {
        List<VentasPorCategoria> c = Rankings.categoriasPrincipales(Arrays.asList(
                cat("Bebidas", 500), cat("Botanas", 300), cat("Lácteos", 100),
                cat("Limpieza", 50), cat("Dulces", 30), cat("Cero", 0)), 4);
        assertEquals(4, c.size());
        assertEquals("Bebidas", c.get(0).categoria);
        assertEquals(Rankings.OTRAS, c.get(3).categoria);
        assertEquals(80, c.get(3).ventas, D);                   // Limpieza + Dulces
        assertEquals(980, Rankings.total(c), D);
    }

    @Test
    public void hueso_cuenta_desde_la_ultima_venta_o_el_alta() {
        ProductoHueso h = new ProductoHueso();
        h.creadoMs = 1000;
        h.existencia = 12;
        h.costo = 20;
        assertEquals(1000, h.quietoDesdeMs());
        h.ultimaVentaMs = 5000L;
        assertEquals(5000, h.quietoDesdeMs());
        assertEquals(240, h.dineroDetenido(), D);
        assertEquals(45, Rankings.diasEntre(0, 45L * 24 * 60 * 60 * 1000 + 5000));
    }
}
