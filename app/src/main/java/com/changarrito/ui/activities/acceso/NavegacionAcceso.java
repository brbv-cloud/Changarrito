package com.changarrito.ui.activities.acceso;

import android.app.Activity;
import android.content.Intent;

import com.changarrito.auth.AuthRepository;
import com.changarrito.ui.activities.ProductosActivity;

/**
 * Decide a qué pantalla va el usuario según su sesión:
 *  - sin sesión → Login
 *  - con sesión de correo sin verificar → Verificar correo
 *  - con sesión lista → la app (por ahora Productos).
 *
 * TODO Sprint 6: si todavía no hay ConfiguracionNegocio, ir a "Elige tu giro"
 * antes de entrar a la app.
 */
public final class NavegacionAcceso {

    private NavegacionAcceso() {
    }

    /** Lleva a la pantalla que toca y borra el historial, para que "atrás" no regrese al login. */
    public static void irAlDestino(Activity desde, AuthRepository auth) {
        Class<?> destino;
        if (auth.getUsuarioActual() == null) {
            destino = LoginActivity.class;
        } else if (auth.necesitaVerificarCorreo()) {
            destino = VerificarCorreoActivity.class;
        } else {
            destino = ProductosActivity.class;
        }
        Intent intent = new Intent(desde, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        desde.startActivity(intent);
        desde.finish();
    }
}
