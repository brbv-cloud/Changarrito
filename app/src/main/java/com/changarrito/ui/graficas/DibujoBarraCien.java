package com.changarrito.ui.graficas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Locale;

/**
 * Barra horizontal "De cada $100 que cobras": la parte gris es lo que se usa para
 * volver a surtir y la verde es la ganancia. Si se vendio bajo costo, toda la barra
 * va en rojo.
 */
public final class DibujoBarraCien {

    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private DibujoBarraCien() {
    }

    public static void dibujar(Canvas c, RectF area, double repones, double ganas, EstiloGrafica e) {
        dibujar(c, area, repones, ganas, false, e);
    }

    /** @param enteros true = "$77" (app, mas facil de leer); false = "$76.88" (PDF). */
    public static void dibujar(Canvas c, RectF area, double repones, double ganas, boolean enteros, EstiloGrafica e) {
        String fmt = enteros ? "$%.0f" : "$%.2f";
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = Math.min(area.height() / 2f, e.radio * 2);

        if (ganas < 0) {
            p.setColor(e.colorPerdida);
            c.drawRoundRect(area, r, r, p);
            textoCentrado(c, area, "Pierdes " + String.format(MX, fmt, -ganas), 0xFFFFFFFF, e);
            return;
        }

        float corte = area.left + area.width() * (float) Math.max(0, Math.min(100, repones)) / 100f;
        // Fondo verde completo y encima la parte gris: asi cada lado conserva su esquina redonda.
        p.setColor(e.colorGanas);
        c.drawRoundRect(area, r, r, p);
        if (corte > area.left) {
            p.setColor(e.colorRepones);
            c.save();
            c.clipRect(area.left, area.top, corte, area.bottom);
            c.drawRoundRect(area, r, r, p);
            c.restore();
        }

        RectF izq = new RectF(area.left, area.top, corte, area.bottom);
        RectF der = new RectF(corte, area.top, area.right, area.bottom);
        textoSiCabe(c, izq, String.format(MX, fmt, repones), e.colorTexto, e);
        textoSiCabe(c, der, String.format(MX, fmt, ganas), 0xFFFFFFFF, e);
    }

    private static void textoSiCabe(Canvas c, RectF zona, String texto, int color, EstiloGrafica e) {
        Paint t = pintura(color, e);
        if (t.measureText(texto) + e.texto < zona.width()) textoCentrado(c, zona, texto, color, e);
    }

    private static void textoCentrado(Canvas c, RectF zona, String texto, int color, EstiloGrafica e) {
        Paint t = pintura(color, e);
        Paint.FontMetrics fm = t.getFontMetrics();
        float y = zona.centerY() - (fm.ascent + fm.descent) / 2f;
        c.drawText(texto, zona.centerX(), y, t);
    }

    private static Paint pintura(int color, EstiloGrafica e) {
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(color);
        t.setTextSize(e.texto * 1.1f);
        t.setFakeBoldText(true);
        t.setTextAlign(Paint.Align.CENTER);
        return t;
    }
}
