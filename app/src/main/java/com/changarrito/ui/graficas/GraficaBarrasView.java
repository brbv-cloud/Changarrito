package com.changarrito.ui.graficas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/** Barras sencillas en pantalla (ventas por dia, hora pico). Al tocar una barra se marca y muestra su monto. */
public class GraficaBarrasView extends View {

    public interface OnBarraTocada {
        void onBarraTocada(int indice);
    }

    private final EstiloGrafica estilo;
    private final RectF area = new RectF();
    private String[] etiquetas = new String[0];
    private double[] valores = new double[0];
    private int destacada = -1;
    private int seleccionada = -1;
    private int colorFuerte;
    private DibujoBarras.Formato formato = v -> String.valueOf((long) v);
    private float[] centros = new float[0];
    private OnBarraTocada listener;

    public GraficaBarrasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        estilo = EstiloGrafica.paraPantalla(context);
        colorFuerte = estilo.colorGanas;
        setClickable(true);
    }

    public void setDatos(String[] etiquetas, double[] valores, int destacada, int colorFuerte,
                         DibujoBarras.Formato formato) {
        this.etiquetas = etiquetas != null ? etiquetas : new String[0];
        this.valores = valores != null ? valores : new double[0];
        this.destacada = destacada;
        this.seleccionada = -1;
        this.colorFuerte = colorFuerte;
        if (formato != null) this.formato = formato;
        invalidate();
    }

    public void setOnBarraTocada(OnBarraTocada l) {
        listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        area.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        centros = DibujoBarras.dibujar(canvas, area, etiquetas, valores, destacada, seleccionada, colorFuerte, formato, estilo);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (centros.length == 0) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int cercano = 0;
            for (int i = 1; i < centros.length; i++) {
                if (Math.abs(centros[i] - event.getX()) < Math.abs(centros[cercano] - event.getX())) cercano = i;
            }
            seleccionada = cercano == destacada ? -1 : cercano;
            invalidate();
            if (listener != null) listener.onBarraTocada(cercano);
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
