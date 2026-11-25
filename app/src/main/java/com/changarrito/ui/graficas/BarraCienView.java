package com.changarrito.ui.graficas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** "De cada $100 que cobras" en pantalla. */
public class BarraCienView extends View {

    private final EstiloGrafica estilo;
    private final RectF area = new RectF();
    private double repones;
    private double ganas;

    public BarraCienView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        estilo = EstiloGrafica.paraPantalla(context);
    }

    /** @param reparto {repones, ganas} de ReparticionPesos.deCada100 */
    public void setReparto(double[] reparto) {
        if (reparto == null) return;
        repones = reparto[0];
        ganas = reparto[1];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        area.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        DibujoBarraCien.dibujar(canvas, area, repones, ganas, true, estilo);
    }
}
