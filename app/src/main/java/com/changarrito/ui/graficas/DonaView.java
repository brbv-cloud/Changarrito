package com.changarrito.ui.graficas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Dona de "ventas por categoria" en pantalla. */
public class DonaView extends View {

    private final EstiloGrafica estilo;
    private final RectF area = new RectF();
    private double[] valores = new double[0];
    private int[] colores = new int[]{0xFF0B8478};
    private String centroGrande;
    private String centroChico;

    public DonaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        estilo = EstiloGrafica.paraPantalla(context);
    }

    public void setDatos(double[] valores, int[] colores, String centroGrande, String centroChico) {
        this.valores = valores != null ? valores : new double[0];
        if (colores != null && colores.length > 0) this.colores = colores;
        this.centroGrande = centroGrande;
        this.centroChico = centroChico;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        area.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        DibujoDona.dibujar(canvas, area, valores, colores, centroGrande, centroChico, estilo);
    }
}
