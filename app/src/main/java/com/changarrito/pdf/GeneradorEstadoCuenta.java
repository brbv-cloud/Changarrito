package com.changarrito.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import androidx.core.content.ContextCompat;

import com.changarrito.R;
import com.changarrito.database.dao.CierreConUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.DatosEstadoCuenta;
import com.changarrito.reportes.DiaReporte;
import com.changarrito.reportes.EscalaGrafica;
import com.changarrito.reportes.Rankings;
import com.changarrito.reportes.ReparticionPesos;
import com.changarrito.reportes.SerieDiaria;
import com.changarrito.repository.MermaRepository;
import com.changarrito.ui.graficas.DibujoBarraCien;
import com.changarrito.ui.graficas.DibujoBarras;
import com.changarrito.ui.graficas.DibujoDona;
import com.changarrito.ui.graficas.DibujoGraficaDiaria;
import com.changarrito.ui.graficas.EstiloGrafica;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Genera el PDF "Estado de cuenta" (el reporte completo; la app muestra solo lo esencial) con PdfDocument (incluido en Android, sin
 * librerias). Hoja tamano carta en puntos (612 × 792). Escribe de arriba hacia
 * abajo con un "cursor" y salta de pagina cuando algo ya no cabe.
 * Las graficas se dibujan con las mismas clases que la pantalla.
 */
public class GeneradorEstadoCuenta {

    private static final float ANCHO = 612;
    private static final float ALTO = 792;
    private static final float MARGEN = 40;
    private static final float PIE = 34;
    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private final Context ctx;
    private final EstiloGrafica estiloGrafica;
    private final int verde, verdeOscuro, texto, textoSuave, linea, fondo, error, ambar;

    private PdfDocument doc;
    private PdfDocument.Page pagina;
    private Canvas c;
    private float y;
    private int numeroPagina;
    private String encabezadoCorto;

    public GeneradorEstadoCuenta(Context ctx) {
        this.ctx = ctx;
        estiloGrafica = EstiloGrafica.paraPdf(ctx);
        verde = ContextCompat.getColor(ctx, R.color.marca_verde);
        verdeOscuro = ContextCompat.getColor(ctx, R.color.marca_verde_oscuro);
        texto = ContextCompat.getColor(ctx, R.color.acceso_texto);
        textoSuave = ContextCompat.getColor(ctx, R.color.acceso_texto_secundario);
        linea = ContextCompat.getColor(ctx, R.color.acceso_borde);
        fondo = ContextCompat.getColor(ctx, R.color.acceso_fondo);
        error = ContextCompat.getColor(ctx, R.color.acceso_error);
        ambar = ContextCompat.getColor(ctx, R.color.status_ambar);
    }

    public int getPaginas() {
        return numeroPagina;
    }

    public File generar(DatosEstadoCuenta d) throws IOException {
        doc = new PdfDocument();
        numeroPagina = 0;
        String negocio = d.configuracion != null && d.configuracion.nombreNegocio != null
                ? d.configuracion.nombreNegocio : "Mi negocio";
        encabezadoCorto = "Estado de cuenta · " + negocio + " · " + periodo(d);
        try {
            nuevaPagina(true, d, negocio);
            resumen(d);
            deCada100(d.resumen);
            if (d.dias.size() > 1) graficaPorDia(d.dias);
            tablaPorDia(d.dias);
            horaPico(d.porHora);
            topDiez(d.productos);
            categorias(d.porCategoria);
            tablaProductos(d.productos);
            huesos(d.huesos, d.generadoMs);
            mermas(d.mermasPorMotivo, d.mermas);
            tablaPersonas(d.personas);
            tablaCierres(d.cierres);
            notas();
            cerrarPagina();

            File dir = new File(ctx.getCacheDir(), "reportes");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("No se pudo crear la carpeta temporal");
            File[] viejos = dir.listFiles();
            if (viejos != null) {
                for (File f : viejos) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
            File salida = new File(dir, nombreArchivo(negocio, d));
            try (FileOutputStream out = new FileOutputStream(salida)) {
                doc.writeTo(out);
            }
            return salida;
        } finally {
            doc.close();
        }
    }

    // ------------------------------------------------------------------ paginas

    private void nuevaPagina(boolean primera, DatosEstadoCuenta d, String negocio) {
        numeroPagina++;
        pagina = doc.startPage(new PdfDocument.PageInfo.Builder((int) ANCHO, (int) ALTO, numeroPagina).create());
        c = pagina.getCanvas();
        y = MARGEN;
        if (primera) {
            encabezadoCompleto(d, negocio);
        } else {
            Paint p = pintura(7.5f, textoSuave, false);
            c.drawText(encabezadoCorto, MARGEN, y + 8, p);
            y += 16;
            lineaHorizontal(verde, 1f);
            y += 12;
        }
    }

    private void cerrarPagina() {
        Paint p = pintura(7.5f, textoSuave, false);
        float yPie = ALTO - MARGEN + 14;
        c.drawLine(MARGEN, yPie - 12, ANCHO - MARGEN, yPie - 12, linea(0.5f));
        c.drawText("Generado con Changarrito", MARGEN, yPie, p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Página " + numeroPagina, ANCHO - MARGEN, yPie, p);
        doc.finishPage(pagina);
    }

    /** Si no caben "alto" puntos mas, termina la pagina y empieza otra. */
    private boolean asegurarEspacio(float alto) {
        if (y + alto <= ALTO - MARGEN - PIE) return false;
        cerrarPagina();
        nuevaPagina(false, null, null);
        return true;
    }

    private void encabezadoCompleto(DatosEstadoCuenta d, String negocio) {
        Bitmap logo = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo_changarrito);
        float tamLogo = 46;
        if (logo != null) {
            c.drawBitmap(logo, null, new RectF(MARGEN, y, MARGEN + tamLogo, y + tamLogo), new Paint(Paint.FILTER_BITMAP_FLAG));
        }
        float x = MARGEN + tamLogo + 12;
        c.drawText("Estado de cuenta", x, y + 18, pintura(18, verdeOscuro, true));
        c.drawText(negocio, x, y + 33, pintura(11, texto, true));
        String giro = d.configuracion != null && d.configuracion.giro != null ? d.configuracion.giro.nombreVisible : "";
        c.drawText(giro, x, y + 45, pintura(8.5f, textoSuave, false));

        Paint der = pintura(8.5f, textoSuave, false);
        der.setTextAlign(Paint.Align.RIGHT);
        Paint derFuerte = pintura(10, texto, true);
        derFuerte.setTextAlign(Paint.Align.RIGHT);
        float xd = ANCHO - MARGEN;
        c.drawText("Periodo", xd, y + 12, der);
        c.drawText(periodo(d), xd, y + 26, derFuerte);
        c.drawText("Generado el " + new SimpleDateFormat("dd/MM/yyyy HH:mm", MX).format(new Date(d.generadoMs)),
                xd, y + 40, der);
        y += tamLogo + 10;
        lineaHorizontal(verde, 1.5f);
        y += 16;
    }

    // ------------------------------------------------------------------ secciones

    private void titulo(String t) {
        asegurarEspacio(40);
        c.drawText(t, MARGEN, y + 11, pintura(11.5f, verdeOscuro, true));
        y += 20;
    }

    private void resumen(DatosEstadoCuenta d) {
        ResumenGanancia r = d.resumen;
        titulo("Resumen del periodo");
        double ganancia = CalculoGanancias.ganancia(r);
        float anchoCaja = (ANCHO - 2 * MARGEN - 2 * 10) / 3f;
        caja(MARGEN, "Vendiste", CalculoGanancias.dinero(r.ventas), texto, anchoCaja);
        caja(MARGEN + anchoCaja + 10, "Te costó", CalculoGanancias.dinero(r.costo), texto, anchoCaja);
        caja(MARGEN + 2 * (anchoCaja + 10), "Te quedó", CalculoGanancias.dinero(ganancia),
                ganancia < 0 ? error : verde, anchoCaja);
        y += 56;

        Paint p = pintura(9.5f, texto, false);
        String ventas = r.tickets + (r.tickets == 1 ? " compra" : " compras") + "   ·   ticket promedio "
                + CalculoGanancias.dinero(Rankings.ticketPromedio(r));
        if (r.costo > 0) {
            c.drawText(CalculoGanancias.porcentaje(CalculoGanancias.pctSobreCosto(r)) + " sobre el costo   ·   "
                    + CalculoGanancias.porcentaje(CalculoGanancias.pctDelPrecio(r)) + " del precio de venta   ·   "
                    + ventas, MARGEN, y + 10, p);
        } else {
            c.drawText(ventas, MARGEN, y + 10, p);
        }
        y += 18;
        if (r.ventasSinCosto > 0) {
            String aviso = r.ventasSinCosto + (r.ventasSinCosto == 1 ? " venta" : " ventas") + " ("
                    + CalculoGanancias.dinero(r.importeSinCosto)
                    + ") de productos sin costo registrado no se cuentan en la ganancia.";
            c.drawText(aviso, MARGEN, y + 9, pintura(8.5f, ambar, false));
            y += 16;
        }
        y += 8;
    }

    private void caja(float x, String etiqueta, String valor, int colorValor, float ancho) {
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(fondo);
        c.drawRoundRect(new RectF(x, y, x + ancho, y + 46), 6, 6, f);
        c.drawText(etiqueta, x + 10, y + 15, pintura(8.5f, textoSuave, false));
        c.drawText(valor, x + 10, y + 35, pintura(15, colorValor, true));
    }

    private void deCada100(ResumenGanancia r) {
        double[] reparto = ReparticionPesos.deCada100(r.ventasConCosto, r.costo);
        if (reparto == null) return;
        asegurarEspacio(80);
        titulo("De cada $100 que cobras");
        RectF barra = new RectF(MARGEN, y, ANCHO - MARGEN, y + 20);
        DibujoBarraCien.dibujar(c, barra, reparto[0], reparto[1], estiloGrafica);
        y += 30;
        String explica = reparto[1] < 0
                ? String.format(MX, "Se vendió por debajo del costo: por cada $100 cobrados se pierden $%.2f.", -reparto[1])
                : String.format(MX, "$%.2f son para volver a surtir la mercancía y $%.2f son tu ganancia.",
                reparto[0], reparto[1]);
        c.drawText(explica, MARGEN, y + 8, pintura(9, texto, false));
        y += 24;
    }

    private void graficaPorDia(List<DiaReporte> dias) {
        asegurarEspacio(230);
        titulo("Día por día");
        float xLey = MARGEN;
        xLey = leyenda(xLey, estiloGrafica.colorRepones, "Para volver a surtir");
        xLey = leyenda(xLey, estiloGrafica.colorGanas, "Tu ganancia");
        leyenda(xLey, estiloGrafica.colorSinCosto, "Sin costo registrado");
        y += 14;
        DibujoGraficaDiaria.dibujar(c, new RectF(MARGEN, y, ANCHO - MARGEN, y + 170), dias, true, -1, estiloGrafica);
        y += 184;
    }

    private float leyenda(float x, int color, String t) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        c.drawRoundRect(new RectF(x, y + 1, x + 9, y + 9), 2, 2, p);
        Paint tp = pintura(8, textoSuave, false);
        c.drawText(t, x + 13, y + 8.5f, tp);
        return x + 13 + tp.measureText(t) + 16;
    }

    private void tablaPorDia(List<DiaReporte> dias) {
        List<DiaReporte> conVentas = new ArrayList<>();
        for (DiaReporte d : dias) if (d.ventas > 0.004) conVentas.add(d);
        titulo("Detalle por día");
        if (conVentas.isEmpty()) {
            sinDatos("No hubo ventas en este periodo.");
            return;
        }
        Tabla t = new Tabla(new String[]{"Día", "Vendiste", "Repones", "Ganas", "% costo", "% precio"},
                new float[]{1.6f, 1, 1, 1, 0.8f, 0.8f},
                new boolean[]{false, true, true, true, true, true});
        SimpleDateFormat fmt = new SimpleDateFormat("EEE dd/MM/yyyy", MX);
        t.encabezado();
        for (DiaReporte d : conVentas) {
            t.fila(false, fmt.format(new Date(d.diaMs)).replace(".", ""), CalculoGanancias.dinero(d.ventas),
                    d.costo > 0 ? CalculoGanancias.dinero(d.costo) : "—",
                    d.costo > 0 ? CalculoGanancias.dinero(d.ganancia()) : "—",
                    d.costo > 0 ? pct(d.costo, d.ventasConCosto, true) : "—",
                    d.costo > 0 ? pct(d.costo, d.ventasConCosto, false) : "—");
        }
        DiaReporte tot = SerieDiaria.total(dias);
        t.fila(true, "Total", CalculoGanancias.dinero(tot.ventas), CalculoGanancias.dinero(tot.costo),
                CalculoGanancias.dinero(tot.ganancia()),
                tot.costo > 0 ? pct(tot.costo, tot.ventasConCosto, true) : "—",
                tot.costo > 0 ? pct(tot.costo, tot.ventasConCosto, false) : "—");
        y += 14;
    }

    private void tablaProductos(List<GananciaProducto> productos) {
        titulo("Ganancia por producto");
        if (productos == null || productos.isEmpty()) {
            sinDatos("No se vendieron productos en este periodo.");
            return;
        }
        Tabla t = new Tabla(new String[]{"Producto", "Cantidad", "Vendiste", "Costo", "Ganancia", "% costo", "% precio"},
                new float[]{2.2f, 0.9f, 1, 1, 1, 0.75f, 0.75f},
                new boolean[]{false, true, true, true, true, true, true});
        t.encabezado();
        for (GananciaProducto p : productos) {
            boolean conCosto = p.tieneCosto();
            t.fila(false, p.nombre, cantidad(p.cantidad, p.unidadMedida), CalculoGanancias.dinero(p.ventas),
                    conCosto ? CalculoGanancias.dinero(p.costo) : "—",
                    conCosto ? CalculoGanancias.dinero(p.ganancia()) : "Sin costo",
                    conCosto ? CalculoGanancias.porcentaje(CalculoGanancias.pctSobreCosto(p)) : "—",
                    conCosto ? CalculoGanancias.porcentaje(CalculoGanancias.pctDelPrecio(p)) : "—");
        }
        y += 14;
    }

    private void tablaPersonas(List<CalculoGanancias.FilaPersona> personas) {
        titulo("Ventas y cierres por persona");
        if (personas == null || personas.isEmpty()) {
            sinDatos("Sin movimientos en este periodo.");
            return;
        }
        Tabla t = new Tabla(new String[]{"Persona", "Ventas", "Importe", "Cierres", "Faltó", "Sobró"},
                new float[]{2, 0.8f, 1.1f, 0.8f, 1, 1},
                new boolean[]{false, true, true, true, true, true});
        t.encabezado();
        for (CalculoGanancias.FilaPersona f : personas) {
            t.fila(false, f.nombre != null ? f.nombre : "Sin turno", String.valueOf(f.transacciones),
                    CalculoGanancias.dinero(f.ventas), String.valueOf(f.cierres),
                    f.faltante < -0.005 ? CalculoGanancias.dinero(-f.faltante) : "—",
                    f.sobrante > 0.005 ? CalculoGanancias.dinero(f.sobrante) : "—");
        }
        y += 14;
    }

    private void tablaCierres(List<CierreConUsuario> cierres) {
        titulo("Cierres de caja");
        if (cierres == null || cierres.isEmpty()) {
            sinDatos("No se hicieron cierres de caja en este periodo.");
            return;
        }
        Tabla t = new Tabla(new String[]{"Fecha y hora", "Quién", "Sistema", "Contado", "Diferencia", "Notas"},
                new float[]{1.3f, 1.2f, 0.9f, 0.9f, 0.9f, 1.6f},
                new boolean[]{false, false, true, true, true, false});
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", MX);
        t.encabezado();
        for (CierreConUsuario cu : cierres) {
            double dif = cu.cierre.diferencia;
            String difTexto = Math.abs(dif) < 0.005 ? "Cuadró"
                    : (dif > 0 ? "+" : "") + CalculoGanancias.dinero(dif);
            t.fila(false, fmt.format(new Date(cu.cierre.timestampCreacionMs)),
                    cu.nombreUsuario != null ? cu.nombreUsuario : "Sin turno",
                    CalculoGanancias.dinero(cu.cierre.efectivoSistema),
                    CalculoGanancias.dinero(cu.cierre.efectivoFisicoContado), difTexto,
                    cu.cierre.notas != null ? cu.cierre.notas : "");
        }
        y += 14;
    }

    private void notas() {
        asegurarEspacio(60);
        lineaHorizontal(linea, 0.5f);
        y += 12;
        Paint p = pintura(7.5f, textoSuave, false);
        c.drawText("% sobre el costo: cuánto ganas por cada peso que pagaste por la mercancía.", MARGEN, y + 8, p);
        c.drawText("% del precio de venta: qué parte de lo que cobras es ganancia.", MARGEN, y + 19, p);
        c.drawText("Los costos son los que tenía cada producto al momento de venderse.", MARGEN, y + 30, p);
        y += 38;
    }

    private void sinDatos(String t) {
        c.drawText(t, MARGEN, y + 9, pintura(9, textoSuave, false));
        y += 24;
    }

    // ------------------------------------------------------------------ v6: secciones nuevas

    private void horaPico(List<VentasPorHora> datos) {
        int[] porHora = Rankings.ticketsPorHora(datos);
        int pico = Rankings.horaPico(porHora);
        asegurarEspacio(200);
        titulo("Hora pico");
        if (pico < 0) {
            sinDatos("No hubo ventas en este periodo.");
            return;
        }
        c.drawText("Tu hora más fuerte es de " + Rankings.etiquetaHora(pico) + " a "
                        + Rankings.etiquetaHora((pico + 1) % 24) + " (" + porHora[pico] + " compras).",
                MARGEN, y + 9, pintura(9.5f, texto, false));
        y += 18;
        int[] visibles = Rankings.horasVisibles(porHora);
        int n = visibles[1] - visibles[0] + 1;
        String[] etiquetas = new String[n];
        double[] valores = new double[n];
        for (int i = 0; i < n; i++) {
            etiquetas[i] = Rankings.etiquetaHora(visibles[0] + i).replace(" ", "");
            valores[i] = porHora[visibles[0] + i];
        }
        EstiloGrafica e = estiloGrafica;
        e.anchoMaxBarra = 18;
        DibujoBarras.dibujar(c, new RectF(MARGEN, y, ANCHO - MARGEN, y + 130), etiquetas, valores,
                pico - visibles[0], -1, ContextCompat.getColor(ctx, R.color.marca_ambar), v -> String.valueOf((long) v), e);
        e.anchoMaxBarra = 26;
        y += 146;
    }

    private void topDiez(List<GananciaProducto> productos) {
        List<GananciaProducto> vendidos = Rankings.masVendidos(productos, Rankings.TOP);
        titulo("Top 10 más vendidos (por cantidad)");
        if (vendidos.isEmpty()) {
            sinDatos("No se vendieron productos en este periodo.");
        } else {
            Tabla t = new Tabla(new String[]{"#", "Producto", "Cantidad", "Vendiste"},
                    new float[]{0.35f, 3, 1, 1}, new boolean[]{false, false, true, true});
            t.encabezado();
            for (int i = 0; i < vendidos.size(); i++) {
                GananciaProducto p = vendidos.get(i);
                t.fila(false, String.valueOf(i + 1), p.nombre, cantidad(p.cantidad, p.unidadMedida),
                        CalculoGanancias.dinero(p.ventas));
            }
            y += 14;
        }

        List<GananciaProducto> rentables = Rankings.masRentables(productos, Rankings.TOP);
        titulo("Top 10 más rentables (por ganancia)");
        if (rentables.isEmpty()) {
            sinDatos("No hay ventas de productos con costo registrado.");
            return;
        }
        Tabla t = new Tabla(new String[]{"#", "Producto", "Vendiste", "Ganancia", "% costo", "% precio"},
                new float[]{0.35f, 2.6f, 1, 1, 0.8f, 0.8f}, new boolean[]{false, false, true, true, true, true});
        t.encabezado();
        for (int i = 0; i < rentables.size(); i++) {
            GananciaProducto p = rentables.get(i);
            t.fila(false, String.valueOf(i + 1), p.nombre, CalculoGanancias.dinero(p.ventas),
                    CalculoGanancias.dinero(p.ganancia()),
                    CalculoGanancias.porcentaje(CalculoGanancias.pctSobreCosto(p)),
                    CalculoGanancias.porcentaje(CalculoGanancias.pctDelPrecio(p)));
        }
        y += 14;
    }

    private void categorias(List<VentasPorCategoria> datos) {
        List<VentasPorCategoria> cats = Rankings.categoriasPrincipales(datos, 6);
        asegurarEspacio(170);
        titulo("Ventas por categoría");
        if (cats.isEmpty()) {
            sinDatos("No hubo ventas en este periodo.");
            return;
        }
        int[] paleta = {
                ContextCompat.getColor(ctx, R.color.categoria_1), ContextCompat.getColor(ctx, R.color.categoria_2),
                ContextCompat.getColor(ctx, R.color.categoria_3), ContextCompat.getColor(ctx, R.color.categoria_4),
                ContextCompat.getColor(ctx, R.color.categoria_5)};
        int otras = ContextCompat.getColor(ctx, R.color.categoria_otras);
        double total = Rankings.total(cats);
        double[] valores = new double[cats.size()];
        int[] colores = new int[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            valores[i] = cats.get(i).ventas;
            colores[i] = Rankings.OTRAS.equals(cats.get(i).categoria) ? otras : paleta[i % paleta.length];
        }
        float lado = 130;
        DibujoDona.dibujar(c, new RectF(MARGEN, y, MARGEN + lado, y + lado), valores, colores,
                EscalaGrafica.dineroCorto(total), "vendido", estiloGrafica);
        float x = MARGEN + lado + 30;
        float yy = y + 14;
        Paint nombre = pintura(9.5f, texto, false);
        Paint cifra = pintura(9.5f, texto, true);
        cifra.setTextAlign(Paint.Align.RIGHT);
        Paint cuadro = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < cats.size(); i++) {
            cuadro.setColor(colores[i]);
            c.drawRoundRect(new RectF(x, yy - 8, x + 10, yy + 2), 2, 2, cuadro);
            c.drawText(recortar(cats.get(i).categoria, 170, nombre), x + 16, yy, nombre);
            c.drawText(String.format(MX, "%.0f%%", total > 0 ? cats.get(i).ventas * 100 / total : 0)
                    + "   " + CalculoGanancias.dinero(cats.get(i).ventas), ANCHO - MARGEN, yy, cifra);
            yy += 19;
        }
        y += Math.max(lado, yy - y) + 14;
    }

    private void huesos(List<ProductoHueso> lista, long ahora) {
        titulo("Productos sin moverse (más de " + DatosEstadoCuenta.DIAS_HUESO + " días)");
        if (lista == null || lista.isEmpty()) {
            sinDatos("Todo el inventario con existencia se ha vendido en los últimos "
                    + DatosEstadoCuenta.DIAS_HUESO + " días.");
            return;
        }
        Tabla t = new Tabla(new String[]{"Producto", "Existencia", "Última venta", "Días quieto", "Dinero detenido"},
                new float[]{2.4f, 1, 1.1f, 0.9f, 1.1f}, new boolean[]{false, true, true, true, true});
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", MX);
        double totalDetenido = 0;
        t.encabezado();
        for (ProductoHueso h : lista) {
            totalDetenido += h.dineroDetenido();
            t.fila(false, h.nombre, cantidad(h.existencia, h.unidadMedida),
                    h.ultimaVentaMs != null ? fmt.format(new Date(h.ultimaVentaMs)) : "Nunca",
                    String.valueOf(Rankings.diasEntre(h.quietoDesdeMs(), ahora)),
                    h.dineroDetenido() > 0 ? CalculoGanancias.dinero(h.dineroDetenido()) : "—");
        }
        t.fila(true, "Total detenido en anaquel", "", "", "", CalculoGanancias.dinero(totalDetenido));
        y += 14;
    }

    private void mermas(List<MermaPorMotivo> porMotivo, List<MermaConUsuario> lista) {
        titulo("Mermas (dañado, caducado, robo)");
        if (lista == null || lista.isEmpty()) {
            sinDatos("No se registraron mermas en este periodo.");
            return;
        }
        double total = 0;
        for (MermaPorMotivo m : porMotivo) total += m.costo;
        c.drawText("Se perdieron " + CalculoGanancias.dinero(total) + " a precio de costo en "
                + lista.size() + (lista.size() == 1 ? " registro." : " registros."), MARGEN, y + 9, pintura(9.5f, error, true));
        y += 20;
        Tabla t = new Tabla(new String[]{"Motivo", "Registros", "Costo perdido"},
                new float[]{2, 1, 1}, new boolean[]{false, true, true});
        t.encabezado();
        for (MermaPorMotivo m : porMotivo) {
            t.fila(false, MotivoMerma.nombre(m.motivo), String.valueOf(m.registros), CalculoGanancias.dinero(m.costo));
        }
        y += 12;
        Tabla d = new Tabla(new String[]{"Fecha", "Producto", "Cantidad", "Motivo", "Quién", "Costo"},
                new float[]{1.1f, 2, 0.9f, 1, 1, 0.9f}, new boolean[]{false, false, true, false, false, true});
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", MX);
        d.encabezado();
        for (MermaConUsuario mu : lista) {
            d.fila(false, fmt.format(new Date(mu.merma.timestampMs)), mu.merma.nombreProducto,
                    MermaRepository.cantidadTexto(mu.merma.cantidad, mu.merma.unidadMedida),
                    MotivoMerma.nombre(mu.merma.motivo != null ? mu.merma.motivo.name() : null),
                    mu.nombreUsuario != null ? mu.nombreUsuario : "Sin turno",
                    mu.merma.costoTotal() > 0 ? CalculoGanancias.dinero(mu.merma.costoTotal()) : "—");
        }
        y += 14;
    }

    // ------------------------------------------------------------------ tabla

    /** Tabla sencilla: encabezado con fondo, filas con linea abajo y salto de pagina automatico. */
    private class Tabla {
        private static final float ALTO_FILA = 17;
        private final String[] columnas;
        private final float[] anchos;
        private final boolean[] derecha;

        Tabla(String[] columnas, float[] pesos, boolean[] derecha) {
            this.columnas = columnas;
            this.derecha = derecha;
            float suma = 0;
            for (float p : pesos) suma += p;
            anchos = new float[pesos.length];
            for (int i = 0; i < pesos.length; i++) anchos[i] = (ANCHO - 2 * MARGEN) * pesos[i] / suma;
        }

        void encabezado() {
            asegurarEspacio(ALTO_FILA * 2);
            Paint f = new Paint();
            f.setColor(fondo);
            c.drawRect(MARGEN, y, ANCHO - MARGEN, y + ALTO_FILA, f);
            escribir(columnas, pintura(8, textoSuave, true));
            y += ALTO_FILA;
        }

        void fila(boolean total, String... valores) {
            if (asegurarEspacio(ALTO_FILA)) encabezado();
            escribir(valores, pintura(8.5f, texto, total));
            y += ALTO_FILA;
            c.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea(total ? 0.9f : 0.4f));
        }

        private void escribir(String[] valores, Paint p) {
            float x = MARGEN;
            for (int i = 0; i < valores.length; i++) {
                float relleno = 5;
                String v = recortar(valores[i] == null ? "" : valores[i], anchos[i] - 2 * relleno, p);
                p.setTextAlign(derecha[i] ? Paint.Align.RIGHT : Paint.Align.LEFT);
                c.drawText(v, derecha[i] ? x + anchos[i] - relleno : x + relleno, y + ALTO_FILA - 5.5f, p);
                x += anchos[i];
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private Paint pintura(float tam, int color, boolean negrita) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tam);
        p.setColor(color);
        p.setTypeface(negrita ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }

    private Paint linea(float grosor) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(linea);
        p.setStrokeWidth(grosor);
        return p;
    }

    private void lineaHorizontal(int color, float grosor) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeWidth(grosor);
        c.drawLine(MARGEN, y, ANCHO - MARGEN, y, p);
    }

    private static String recortar(String t, float ancho, Paint p) {
        if (p.measureText(t) <= ancho) return t;
        int n = p.breakText(t, true, ancho - p.measureText("…"), null);
        return t.substring(0, Math.max(0, n)) + "…";
    }

    private static String pct(double costo, double precio, boolean sobreCosto) {
        return CalculoGanancias.porcentaje(sobreCosto
                ? com.changarrito.utils.CalculoPrecio.margenSobreCosto(costo, precio)
                : com.changarrito.utils.CalculoPrecio.margenSobrePrecio(costo, precio));
    }

    private static String cantidad(double valor, UnidadMedida unidad) {
        String u = unidad != null ? " " + unidad.name() : "";
        if (unidad == UnidadMedida.PZA || Math.abs(valor - Math.rint(valor)) < 0.0005) {
            return String.format(MX, "%.0f%s", valor, u);
        }
        return String.format(MX, "%.3f%s", valor, u);
    }

    private static String periodo(DatosEstadoCuenta d) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", MX);
        String ini = f.format(new Date(d.inicioMs));
        String fin = f.format(new Date(d.finMs));
        return ini.equals(fin) ? ini : ini + " – " + fin;
    }

    /** "Estado_de_cuenta_Abarrotes_Lupita_2026-10-19_al_2026-10-25.pdf" (sin acentos ni simbolos). */
    static String nombreArchivo(String negocio, DatosEstadoCuenta d) {
        String limpio = Normalizer.normalize(negocio, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (limpio.isEmpty()) limpio = "negocio";
        if (limpio.length() > 40) limpio = limpio.substring(0, 40);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String ini = f.format(new Date(d.inicioMs));
        String fin = f.format(new Date(d.finMs));
        return "Estado_de_cuenta_" + limpio + "_" + (ini.equals(fin) ? ini : ini + "_al_" + fin) + ".pdf";
    }
}
