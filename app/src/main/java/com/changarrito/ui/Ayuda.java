package com.changarrito.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Recuerda que ayudas ya vio el usuario en este celular: la bienvenida de la primera
 * vez y la guia con puntos de cada seccion (que sale sola la primera vez que se abre).
 * "Volver a mostrar las ayudas" en Ajustes las olvida todas.
 *
 * Se guardan por cuenta: si en el mismo celular se entra con otra cuenta (otro
 * negocio), la bienvenida y el recorrido vuelven a salir.
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
        return c.getApplicationContext().getSharedPreferences(archivo(), Context.MODE_PRIVATE);
    }

    /** Un archivo por cuenta de Google/correo; sin sesion, el general. */
    private static String archivo() {
        FirebaseUser cuenta = FirebaseAuth.getInstance().getCurrentUser();
        return cuenta != null ? PREFS + "_" + cuenta.getUid() : PREFS;
    }
}
