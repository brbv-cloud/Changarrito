package com.changarrito.ui.graficas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.changarrito.reportes.DiaReporte;
import com.changarrito.reportes.EscalaGrafica;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dibuja la grafica "dia por dia": una barra por dia, del tamano de lo vendido.
 * Con desglose, cada barra se parte en lo que repones (abajo), lo que ganas y lo
 * que vendiste sin costo capturado (arriba). Sin desglose es una sola barra verde.
 *
 * No depende de una View: dibuja en cualquier Canvas, el de la pantalla
 * (GraficaDiariaView) o el de una pagina del PDF (GeneradorEstadoCuenta).
 */
public final class DibujoGraficaDiaria {

    private static final Locale MX = Locale.forLanguageTag("es-MX");
    private static final int MAX_ETIQUETAS_DIA = 8;
    private static final int MAX_BARRAS_CON_MONTO = 10;

    private DibujoGraficaDiaria() {
    }

    /**
     * @param seleccionado indice del dia tocado (o -1): se resalta y siempre muestra su monto.
     * @return el centro X de cada barra, para saber que dia se toco.
     */
    public static float[] dibujar(Canvas c, RectF area, List<DiaReporte> dias, boolean desglose,
                                  int seleccionado, EstiloGrafica e) {
        int n = dias == null ? 0 : dias.size();
        float[] centros = new float[n];
        if (n == 0) return centros;

        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setTextSize(e.textoChico);
        texto.setColor(e.colorTextoSuave);

        double max = 0;
        for (DiaReporte d : dias) max = Math.max(max, d.ventas);
        double techo = EscalaGrafica.techo(max);

        // Margenes: a la izquierda los montos del eje, abajo los dias, arriba los montos de las barras.
        float anchoEje = texto.measureText(EscalaGrafica.dineroCorto(techo)) + e.textoChico * 0.6f;
        float arriba = area.top + e.texto * 1.6f;
        float abajo = area.bottom - e.textoChico * 2.0f;
        float izquierda = area.left + anchoEje;
        float alto = abajo - arriba;

        // Lineas guia en 0, mitad y techo.
        Paint linea = new Paint(Paint.ANTI_ALIAS_FLAG);
        linea.setColor(e.colorLinea);
        linea.setStrokeWidth(e.grosorLinea);
        texto.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 2; i++) {
            double valor = techo * i / 2.0;
            float y = abajo - (float) (alto * i / 2.0);
            c.drawLine(izquierda, y, area.right, y, linea);
            c.drawText(EscalaGrafica.dineroCorto(valor), izquierda - e.textoChico * 0.4f,
                    y + e.textoChico * 0.35f, texto);
        }

        float ranura = (area.right - izquierda) / n;
        float anchoBarra = Math.min(e.anchoMaxBarra, ranura * 0.64f);
        int paso = EscalaGrafica.pasoEtiquetas(n, MAX_ETIQUETAS_DIA);
        SimpleDateFormat fmtDia = new SimpleDateFormat(n <= 7 ? "EEE d" : "d/M", MX);

        Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint monto = new Paint(Paint.ANTI_ALIAS_FLAG);
        monto.setTextSize(e.texto);
        monto.setFakeBoldText(true);
        monto.setTextAlign(Paint.Align.CENTER);
        texto.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) {
            DiaReporte d = dias.get(i);
            float cx = izquierda + ranura * i + ranura / 2f;
            centros[i] = cx;
            float x0 = cx - anchoBarra / 2f;
            float x1 = cx + anchoBarra / 2f;

            if (i == seleccionado) {
                relleno.setColor(e.colorSeleccion);
                c.drawRoundRect(new RectF(cx - ranura / 2f + 1, area.top, cx + ranura / 2f - 1, abajo),
                        e.radio, e.radio, relleno);
            }

            float escala = (float) (alto / techo);
            float y = abajo;
            if (desglose) {
                y = segmento(c, x0, x1, y, (float) d.repones() * escala, e.colorRepones, relleno);
                y = segmento(c, x0, x1, y, (float) d.ganas() * escala, e.colorGanas, relleno);
                y = segmento(c, x0, x1, y, (float) d.sinCosto() * escala, e.colorSinCosto, relleno);
            } else {
                y = segmento(c, x0, x1, y, (float) d.ventas * escala, e.colorGanas, relleno);
            }
            // Puntas redondeadas: un circulo-capuchon solo en la parte de arriba.
            if (y < abajo - 2 * e.radio) redondearPunta(c, x0, x1, y, e, d, desglose, relleno);

            boolean conMonto = d.ventas > 0 && (n <= MAX_BARRAS_CON_MONTO || i == seleccionado);
            if (conMonto) {
                monto.setColor(desglose && d.huboPerdida() ? e.colorPerdida : e.colorTexto);
                c.drawText(EscalaGrafica.dineroCorto(d.ventas), cx, y - e.texto * 0.45f, monto);
            }
            if (i % paso == 0 || i == seleccionado) {
                String etiqueta = fmtDia.format(new Date(d.diaMs)).replace(".", "");
                texto.setColor(i == seleccionado ? e.colorTexto : e.colorTextoSuave);
                c.drawText(etiqueta, cx, abajo + e.textoChico * 1.45f, texto);
            }
        }
        return centros;
    }

    private static float segmento(Canvas c, float x0, float x1, float yBase, float alto, int color, Paint p) {
        if (alto <= 0.01f) return yBase;
        p.setColor(color);
        c.drawRect(x0, yBase - alto, x1, yBase, p);
        return yBase - alto;
    }

    /** Redondea la esquina de arriba de la barra con el color del ultimo pedazo. */
    private static void redondearPunta(Canvas c, float x0, float x1, float yTope, EstiloGrafica e,
                                       DiaReporte d, boolean desglose, Paint p) {
        int color;
        if (!desglose) color = e.colorGanas;
        else if (d.sinCosto() > 0) color = e.colorSinCosto;
        else if (d.ganas() > 0) color = e.colorGanas;
        else color = e.colorRepones;
        p.setColor(color);
        float r = Math.min(e.radio, (x1 - x0) / 2f);
        c.drawRoundRect(new RectF(x0, yTope, x1, yTope + 2 * r), r, r, p);
    }
}
