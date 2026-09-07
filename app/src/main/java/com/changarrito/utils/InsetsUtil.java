package com.changarrito.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * A partir de Android 15 (API 35) el contenido de la app se dibuja "edge-to-edge"
 * por defecto: detrás de la barra de estado (donde viven las notificaciones) y de
 * la barra de navegación, en vez de dejarles su espacio como antes. Sin este ajuste,
 * cualquier elemento pegado al borde superior o inferior de la pantalla (un botón,
 * el inicio de una lista) queda tapado o se encima con esas barras.
 *
 * Este helper le pide a la vista raíz de una pantalla que reserve ese espacio como
 * padding, calculado en tiempo real por el propio sistema (no un número fijo como
 * "48dp" que se ve distinto en cada equipo).
 */
public class InsetsUtil {

    private InsetsUtil() {}

    /**
     * @param arriba  true si esta vista debe dejar espacio para la barra de estado/notificaciones
     * @param abajo   true si esta vista debe dejar espacio para la barra de navegación
     */
    public static void aplicarPaddingBarrasDelSistema(View root, boolean arriba, boolean abajo) {
        int paddingIzq = root.getPaddingLeft();
        int paddingDer = root.getPaddingRight();
        int paddingArribaBase = root.getPaddingTop();
        int paddingAbajoBase = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddingIzq,
                    arriba ? paddingArribaBase + barras.top : paddingArribaBase,
                    paddingDer,
                    abajo ? paddingAbajoBase + barras.bottom : paddingAbajoBase
            );
            return insets;
        });
    }
}
