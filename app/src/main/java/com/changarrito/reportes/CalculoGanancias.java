package com.changarrito.reportes;

import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorUsuario;
import com.changarrito.utils.CalculoPrecio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Logica del reporte de Ganancias, sin Android para probarla con JUnit.
 * Los dos porcentajes salen de CalculoPrecio para que coincidan con el
 * formulario de producto:
 *   % sobre el costo    = ganancia / costo
 *   % del precio de venta = ganancia / lo vendido
 */
public final class CalculoGanancias {

    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private CalculoGanancias() {
    }

    public static double ganancia(ResumenGanancia r) {
        if (r == null) return 0;
        return CalculoPrecio.redondearCentavos(r.ventasConCosto - r.costo);
    }

    public static double pctSobreCosto(ResumenGanancia r) {
        if (r == null) return 0;
        return CalculoPrecio.margenSobreCosto(r.costo, r.ventasConCosto);
    }

    public static double pctDelPrecio(ResumenGanancia r) {
        if (r == null) return 0;
        return CalculoPrecio.margenSobrePrecio(r.costo, r.ventasConCosto);
    }

    public static double pctSobreCosto(GananciaProducto p) {
        return CalculoPrecio.margenSobreCosto(p.costo, p.ventasConCosto);
    }

    public static double pctDelPrecio(GananciaProducto p) {
        return CalculoPrecio.margenSobrePrecio(p.costo, p.ventasConCosto);
    }

    /** Mismo semaforo que el formulario, usando costo y precio promedio del rango. */
    public static CalculoPrecio.Semaforo semaforo(GananciaProducto p) {
        return CalculoPrecio.semaforo(p.costo, p.ventasConCosto);
    }

    /**
     * Productos vendidos en amarillo o rojo (dejan menos de MARGEN_BAJO % sobre
     * el costo, o se vendieron por debajo del costo), del que menos deja al que mas.
     */
    public static List<GananciaProducto> menosRentables(List<GananciaProducto> productos, int maximo) {
        List<GananciaProducto> resultado = new ArrayList<>();
        if (productos == null) return resultado;
        for (GananciaProducto p : productos) {
            if (p.tieneCosto() && semaforo(p) != CalculoPrecio.Semaforo.VERDE) {
                resultado.add(p);
            }
        }
        Collections.sort(resultado, (a, b) -> Double.compare(pctSobreCosto(a), pctSobreCosto(b)));
        return resultado.size() > maximo ? new ArrayList<>(resultado.subList(0, maximo)) : resultado;
    }

    /** Una fila por persona: lo que vendio y como le fue en sus cierres. */
    public static class FilaPersona {
        public Integer idUsuario;
        public String nombre;
        public int transacciones;
        public double ventas;
        public int cierres;
        public double faltante;
        public double sobrante;
    }

    /** Junta ventas y cierres por persona; el orden es de quien mas vendio a quien menos. */
    public static List<FilaPersona> combinarPersonas(List<VentasPorUsuario> ventas,
                                                    List<CierresPorUsuario> cierres) {
        Map<Integer, FilaPersona> porId = new LinkedHashMap<>();
        if (ventas != null) {
            for (VentasPorUsuario v : ventas) {
                FilaPersona f = fila(porId, v.idUsuario, v.nombre);
                f.transacciones += v.transacciones;
                f.ventas += v.ventas;
            }
        }
        if (cierres != null) {
            for (CierresPorUsuario c : cierres) {
                FilaPersona f = fila(porId, c.idUsuario, c.nombre);
                f.cierres += c.cierres;
                f.faltante += c.faltante;
                f.sobrante += c.sobrante;
            }
        }
        List<FilaPersona> lista = new ArrayList<>(porId.values());
        Collections.sort(lista, (a, b) -> Double.compare(b.ventas, a.ventas));
        return lista;
    }

    private static FilaPersona fila(Map<Integer, FilaPersona> porId, Integer id, String nombre) {
        // null (sin turno o perfil borrado) se agrupa en una sola fila con clave -1.
        Integer clave = id != null ? id : -1;
        FilaPersona f = porId.get(clave);
        if (f == null) {
            f = new FilaPersona();
            f.idUsuario = id;
            f.nombre = nombre;
            porId.put(clave, f);
        } else if (f.nombre == null) {
            f.nombre = nombre;
        }
        return f;
    }

    public static String dinero(double valor) {
        String texto = String.format(MX, "$%,.2f", Math.abs(valor));
        return valor < -0.004 ? "-" + texto : texto;
    }

    public static String porcentaje(double valor) {
        return String.format(MX, "%.1f%%", valor);
    }
}
