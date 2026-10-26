package com.changarrito.ui.graficas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.changarrito.reportes.EscalaGrafica;

/**
 * Barras verticales sencillas, de un solo color y sin ejes: lo justo para ver
 * "que dia / que hora vendi mas". La barra destacada (el mejor dia, la hora pico)
 * va en color fuerte; la que el usuario toca se marca en oscuro. Ambas muestran su
 * monto encima; las demas van en un tono suave. Las etiquetas de abajo nunca se
 * enciman: si una choca con la de una barra marcada, no se dibuja.
 */
public final class DibujoBarras {

    public interface Formato {
        String texto(double valor);
    }

    private static final int MAX_ETIQUETAS = 8;
    private static final int MAX_MONTOS = 7;

    private DibujoBarras() {
    }

    /**
     * @param destacada    indice resaltado siempre (mejor dia / hora pico), o -1
     * @param seleccionada indice que toco el usuario, o -1
     * @param colorFuerte  color de la barra destacada
     * @return centro X de cada barra (para saber cual se toco)
     */
    public static float[] dibujar(Canvas c, RectF area, String[] etiquetas, double[] valores, int destacada,
                                  int seleccionada, int colorFuerte, Formato formato, EstiloGrafica e) {
        int n = valores == null ? 0 : valores.length;
        float[] centros = new float[n];
        if (n == 0) return centros;

        double max = 0;
        for (double v : valores) max = Math.max(max, v);
        if (max <= 0) max = 1;

        float arriba = area.top + e.texto * 1.7f;
        float abajo = area.bottom - e.textoChico * 2.0f;
        float alto = abajo - arriba;
        float ranura = area.width() / n;
        float ancho = Math.min(e.anchoMaxBarra, ranura * 0.68f);
        int paso = EscalaGrafica.pasoEtiquetas(n, MAX_ETIQUETAS);

        Paint linea = new Paint(Paint.ANTI_ALIAS_FLAG);
        linea.setColor(e.colorLinea);
        linea.setStrokeWidth(e.grosorLinea);
        c.drawLine(area.left, abajo, area.right, abajo, linea);

        Paint etiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
        etiqueta.setTextSize(e.textoChico);
        etiqueta.setTextAlign(Paint.Align.CENTER);
        Paint barra = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint monto = new Paint(Paint.ANTI_ALIAS_FLAG);
        monto.setTextSize(e.texto);
        monto.setFakeBoldText(true);
        monto.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < n; i++) centros[i] = area.left + ranura * i + ranura / 2f;

        // Etiquetas que van si o si (las barras marcadas) y el espacio que ocupan.
        float separacion = e.textoChico * 0.6f;
        java.util.List<float[]> ocupado = new java.util.ArrayList<>();
        boolean etiquetaSeleccionada = true;
        for (int i : new int[]{destacada, seleccionada}) {
            if (i >= 0 && i < n && etiquetas != null && i < etiquetas.length) {
                etiqueta.setFakeBoldText(true);
                float w = etiqueta.measureText(etiquetas[i]) / 2f;
                float x = Math.max(area.left + w, Math.min(area.right - w, centros[i]));
                float[] zona = {x - w - separacion, x + w + separacion};
                // La tocada junto a la destacada: su etiqueta se omite (el detalle sale abajo).
                if (i == seleccionada && i != destacada) {
                    for (float[] o : ocupado) {
                        if (zona[1] > o[0] && zona[0] < o[1]) etiquetaSeleccionada = false;
                    }
                    if (!etiquetaSeleccionada) continue;
                }
                ocupado.add(zona);
            }
        }

        for (int i = 0; i < n; i++) {
            float cx = centros[i];
            boolean esDestacada = i == destacada;
            boolean esSeleccionada = i == seleccionada;
            boolean marcada = esDestacada || esSeleccionada;
            float h = (float) (alto * valores[i] / max);
            if (valores[i] > 0 && h < e.radio) h = e.radio; // que no desaparezca una venta chiquita
            if (h > 0) {
                barra.setColor(esDestacada ? colorFuerte : esSeleccionada ? e.colorTexto : e.colorSuave);
                RectF r = new RectF(cx - ancho / 2f, abajo - h, cx + ancho / 2f, abajo);
                c.drawRoundRect(r, e.radio, e.radio, barra);
                if (h > e.radio) c.drawRect(r.left, abajo - e.radio, r.right, abajo, barra); // base recta
            }
            if (valores[i] > 0 && (marcada || n <= MAX_MONTOS)) {
                monto.setColor(marcada ? e.colorTexto : e.colorTextoSuave);
                c.drawText(formato.texto(valores[i]), cx, abajo - h - e.texto * 0.45f, monto);
            }
            boolean conEtiqueta = !(esSeleccionada && !esDestacada && !etiquetaSeleccionada);
            if (conEtiqueta && etiquetas != null && i < etiquetas.length && (i % paso == 0 || marcada)) {
                etiqueta.setFakeBoldText(marcada);
                float w = etiqueta.measureText(etiquetas[i]) / 2f;
                // Si queda pegada a un borde se recorre hacia dentro (no se sale de la grafica).
                float x = Math.max(area.left + w, Math.min(area.right - w, cx));
                boolean choca = false;
                if (!marcada) {
                    for (float[] o : ocupado) {
                        if (x + w > o[0] && x - w < o[1]) {
                            choca = true;
                            break;
                        }
                    }
                }
                if (!choca) {
                    etiqueta.setColor(marcada ? e.colorTexto : e.colorTextoSuave);
                    c.drawText(etiquetas[i], x, abajo + e.textoChico * 1.45f, etiqueta);
                    if (!marcada) ocupado.add(new float[]{x - w - separacion, x + w + separacion});
                }
            }
        }
        return centros;
    }
}
