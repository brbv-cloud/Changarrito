package com.changarrito.ui.graficas;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.changarrito.R;

/**
 * Colores y tamanos de las graficas. Hay dos juegos: uno para pantalla (en pixeles
 * segun la densidad del celular) y otro para el PDF (en puntos, 72 por pulgada).
 * Asi el mismo dibujo se ve bien en los dos lados.
 */
public class EstiloGrafica {
    public int colorRepones;
    public int colorGanas;
    public int colorSinCosto;
    public int colorPerdida;
    public int colorTexto;
    public int colorTextoSuave;
    public int colorLinea;
    public int colorSeleccion;
    /** Barras no destacadas en las graficas sencillas. */
    public int colorSuave;

    public float texto;
    public float textoChico;
    public float radio;
    public float anchoMaxBarra;
    public float grosorLinea;

    public static EstiloGrafica paraPantalla(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        float d = m.density;
        EstiloGrafica e = colores(c);
        e.texto = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, m);
        e.textoChico = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, m);
        e.radio = 5 * d;
        e.anchoMaxBarra = 34 * d;
        e.grosorLinea = 1 * d;
        return e;
    }

    public static EstiloGrafica paraPdf(Context c) {
        EstiloGrafica e = colores(c);
        e.texto = 8;
        e.textoChico = 7;
        e.radio = 3;
        e.anchoMaxBarra = 26;
        e.grosorLinea = 0.6f;
        return e;
    }

    private static EstiloGrafica colores(Context c) {
        EstiloGrafica e = new EstiloGrafica();
        e.colorRepones = ContextCompat.getColor(c, R.color.grafica_repones);
        e.colorGanas = ContextCompat.getColor(c, R.color.marca_verde);
        e.colorSinCosto = ContextCompat.getColor(c, R.color.grafica_sin_costo);
        e.colorPerdida = ContextCompat.getColor(c, R.color.acceso_error);
        e.colorTexto = ContextCompat.getColor(c, R.color.acceso_texto);
        e.colorTextoSuave = ContextCompat.getColor(c, R.color.acceso_texto_secundario);
        e.colorLinea = ContextCompat.getColor(c, R.color.acceso_borde);
        e.colorSuave = ContextCompat.getColor(c, R.color.grafica_barra_suave);
        e.colorSeleccion = Color.argb(28, Color.red(e.colorGanas), Color.green(e.colorGanas), Color.blue(e.colorGanas));
        return e;
    }
}
