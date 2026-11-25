package com.changarrito.reportes;

import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Calculos de los reportes sencillos: ticket promedio, top 10, hora pico y categorias. */
public final class Rankings {

    public static final int TOP = 10;
    public static final String OTRAS = "Otras";

    private Rankings() {
    }

    /** Lo que gasta en promedio cada cliente por compra. */
    public static double ticketPromedio(ResumenGanancia r) {
        if (r == null || r.tickets <= 0) return 0;
        return r.ventas / r.tickets;
    }

    /** Los que mas se mueven: por cantidad vendida (piezas, kilos...). */
    public static List<GananciaProducto> masVendidos(List<GananciaProducto> productos, int n) {
        List<GananciaProducto> copia = productos == null ? new ArrayList<>() : new ArrayList<>(productos);
        Collections.sort(copia, (a, b) -> Double.compare(b.cantidad, a.cantidad));
        return copia.size() > n ? new ArrayList<>(copia.subList(0, n)) : copia;
    }

    /** Los que mas dinero dejan (ganancia en pesos), aunque no sean los que mas se venden. */
    public static List<GananciaProducto> masRentables(List<GananciaProducto> productos, int n) {
        List<GananciaProducto> conGanancia = new ArrayList<>();
        if (productos != null) {
            for (GananciaProducto p : productos) {
                if (p.tieneCosto() && p.ganancia() > 0.004) conGanancia.add(p);
            }
        }
        Collections.sort(conGanancia, (a, b) -> Double.compare(b.ganancia(), a.ganancia()));
        return conGanancia.size() > n ? new ArrayList<>(conGanancia.subList(0, n)) : conGanancia;
    }

    // ---------------------------------------------------------------- hora pico

    /** Compras por hora, las 24 horas del dia (las horas sin ventas quedan en 0). */
    public static int[] ticketsPorHora(List<VentasPorHora> datos) {
        int[] horas = new int[24];
        if (datos != null) {
            for (VentasPorHora h : datos) {
                if (h.hora >= 0 && h.hora < 24) horas[h.hora] += h.tickets;
            }
        }
        return horas;
    }

    /** Hora con mas compras, o -1 si no hubo ventas. Si hay empate gana la mas temprana. */
    public static int horaPico(int[] porHora) {
        int mejor = -1;
        for (int h = 0; h < porHora.length; h++) {
            if (porHora[h] > 0 && (mejor < 0 || porHora[h] > porHora[mejor])) mejor = h;
        }
        return mejor;
    }

    /**
     * Horas a mostrar en la grafica: de 7 am a 10 pm, ampliado si hubo ventas antes
     * o despues. Regresa {primera, ultima} inclusive.
     */
    public static int[] horasVisibles(int[] porHora) {
        int primera = 7;
        int ultima = 22;
        for (int h = 0; h < 24; h++) {
            if (porHora[h] > 0) {
                primera = Math.min(primera, h);
                ultima = Math.max(ultima, h);
            }
        }
        return new int[]{primera, ultima};
    }

    /** "7 am", "12 pm", "6 pm", "12 am". */
    public static String etiquetaHora(int h) {
        int hora12 = h % 12 == 0 ? 12 : h % 12;
        return hora12 + (h < 12 ? " am" : " pm");
    }

    // ---------------------------------------------------------------- categorias

    /**
     * Las categorias que mas venden y el resto juntas en "Otras", para que la grafica
     * de pastel no tenga 20 rebanadas diminutas. Ordenadas de mayor a menor.
     */
    public static List<VentasPorCategoria> categoriasPrincipales(List<VentasPorCategoria> datos, int maximo) {
        List<VentasPorCategoria> copia = new ArrayList<>();
        if (datos != null) {
            for (VentasPorCategoria c : datos) if (c.ventas > 0.004) copia.add(c);
        }
        Collections.sort(copia, (a, b) -> Double.compare(b.ventas, a.ventas));
        if (copia.size() <= maximo) return copia;
        List<VentasPorCategoria> resultado = new ArrayList<>(copia.subList(0, maximo - 1));
        VentasPorCategoria otras = new VentasPorCategoria();
        otras.categoria = OTRAS;
        for (int i = maximo - 1; i < copia.size(); i++) otras.ventas += copia.get(i).ventas;
        resultado.add(otras);
        return resultado;
    }

    public static double total(List<VentasPorCategoria> datos) {
        double t = 0;
        if (datos != null) for (VentasPorCategoria c : datos) t += c.ventas;
        return t;
    }

    /** Dias completos entre dos momentos (para "sin venderse hace 45 dias"). */
    public static int diasEntre(long desdeMs, long hastaMs) {
        return (int) Math.max(0, (hastaMs - desdeMs) / (24L * 60 * 60 * 1000));
    }
}
