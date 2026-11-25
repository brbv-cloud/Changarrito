package com.changarrito.ui;

import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

/**
 * Mensajes cortos de la app ("Venta registrada", "Solo hay 3 PZA"...).
 *
 * Antes eran Toast: duraban hasta 3.5 s, no se podian quitar y tapaban la barra de abajo.
 * Ahora son una barrita que dura 2.5 s, se quita con un toque y sale arriba de la
 * barra de navegacion (o de la barra del carrito en Vender), sin tapar nada importante.
 */
public final class Avisos {

    private static final int DURACION_MS = 2500;

    private Avisos() {
    }

    /**
     * @param cualquiera una vista de la pantalla (sirve para encontrar donde dibujarlo)
     * @param ancla      vista arriba de la cual debe salir; null = abajo de la pantalla
     */
    public static void mostrar(View cualquiera, CharSequence mensaje, @Nullable View ancla) {
        if (cualquiera == null || mensaje == null || mensaje.length() == 0) return;
        Snackbar aviso = Snackbar.make(cualquiera, mensaje, Snackbar.LENGTH_SHORT);
        aviso.setDuration(DURACION_MS);
        aviso.setTextMaxLines(3);
        if (ancla != null && ancla.isShown()) aviso.setAnchorView(ancla);
        // Un toque lo quita (deslizarlo no siempre se puede fuera de un CoordinatorLayout).
        aviso.getView().setOnClickListener(v -> aviso.dismiss());
        aviso.show();
    }
}
