package com.changarrito.ui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Recuerda que ayudas ya vio el usuario en este celular: la bienvenida de la primera
 * vez y la guia con puntos de cada seccion (que sale sola la primera vez que se abre).
 * "Volver a mostrar las ayudas" en Ajustes las olvida todas.
 */
public final class Ayuda {

    private static final String PREFS = "ayudas_vistas";

    private Ayuda() {
    }

    public static boolean yaVista(Context c, String clave) {
        return prefs(c).getBoolean(clave, false);
    }

    public static void marcarVista(Context c, String clave) {
        prefs(c).edit().putBoolean(clave, true).apply();
    }

    /** Vuelve a mostrar todas las ayudas como la primera vez (Ajustes). */
    public static void reiniciar(Context c) {
        prefs(c).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
