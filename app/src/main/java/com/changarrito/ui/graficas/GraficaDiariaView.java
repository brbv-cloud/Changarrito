package com.changarrito.ui.graficas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.changarrito.reportes.DiaReporte;

import java.util.ArrayList;
import java.util.List;

/** Grafica "dia por dia" en pantalla. Al tocar una barra se resalta y avisa que dia fue. */
public class GraficaDiariaView extends View {

    public interface OnDiaTocado {
        void onDiaTocado(DiaReporte dia);
    }

    private final EstiloGrafica estilo;
    private final RectF area = new RectF();
    private List<DiaReporte> dias = new ArrayList<>();
    private boolean desglose = true;
    private int seleccionado = -1;
    private float[] centros = new float[0];
    private OnDiaTocado listener;

    public GraficaDiariaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        estilo = EstiloGrafica.paraPantalla(context);
        setClickable(true);
    }

    public void setDatos(List<DiaReporte> nuevos, boolean conDesglose) {
        dias = nuevos != null ? nuevos : new ArrayList<>();
        desglose = conDesglose;
        seleccionado = -1;
        invalidate();
    }

    public void setOnDiaTocado(OnDiaTocado l) {
        listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        area.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        centros = DibujoGraficaDiaria.dibujar(canvas, area, dias, desglose, seleccionado, estilo);
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
            seleccionado = cercano;
            invalidate();
            if (listener != null && cercano < dias.size()) listener.onDiaTocado(dias.get(cercano));
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
