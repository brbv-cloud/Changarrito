package com.changarrito.ui.graficas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Grafica de pastel en forma de dona, con el total al centro. */
public final class DibujoDona {

    private DibujoDona() {
    }

    public static void dibujar(Canvas c, RectF area, double[] valores, int[] colores,
                               String centroGrande, String centroChico, EstiloGrafica e) {
        float lado = Math.min(area.width(), area.height());
        float cx = area.centerX();
        float cy = area.centerY();
        float grosor = lado * 0.16f;
        float r = lado / 2f - grosor / 2f;
        RectF aro = new RectF(cx - r, cy - r, cx + r, cy + r);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(grosor);

        double total = 0;
        if (valores != null) for (double v : valores) total += Math.max(0, v);
        if (total <= 0) {
            p.setColor(e.colorLinea);
            c.drawArc(aro, 0, 360, false, p);
        } else {
            float inicio = -90;
            float hueco = valores.length > 1 ? 1.5f : 0;
            for (int i = 0; i < valores.length; i++) {
                float barrido = (float) (360.0 * Math.max(0, valores[i]) / total);
                if (barrido <= 0) continue;
                p.setColor(colores[i % colores.length]);
                c.drawArc(aro, inicio + hueco / 2f, Math.max(0.5f, barrido - hueco), false, p);
                inicio += barrido;
            }
        }

        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTextAlign(Paint.Align.CENTER);
        t.setColor(e.colorTexto);
        t.setFakeBoldText(true);
        t.setTextSize(e.texto * 1.35f);
        if (centroGrande != null) c.drawText(centroGrande, cx, cy + e.texto * 0.2f, t);
        t.setFakeBoldText(false);
        t.setTextSize(e.textoChico);
        t.setColor(e.colorTextoSuave);
        if (centroChico != null) c.drawText(centroChico, cx, cy + e.texto * 1.5f, t);
    }
}
