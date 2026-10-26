package com.changarrito.ui.activities.acceso;

import android.app.Activity;
import android.content.Intent;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.ui.activities.PrincipalActivity;
import com.changarrito.ui.activities.negocio.ElegirGiroActivity;
import com.changarrito.ui.activities.negocio.SeleccionarTurnoActivity;
import com.changarrito.ui.activities.negocio.VistaPreviaCatalogoActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * Decide a qué pantalla va el usuario:
 *  - sin sesión → Login
 *  - correo sin verificar → Verificar correo
 *  - primera vez → Elige tu giro → (vista previa del catálogo)
 *  - varias personas en el negocio y nadie con turno → ¿Quién atiende?
 *  - todo listo → la app.
 * Siempre borra el historial, para que "atrás" no regrese a pantallas de acceso.
 */
public final class NavegacionAcceso {

    private NavegacionAcceso() {
    }

    public static void irAlDestino(Activity desde, AuthRepository auth) {
        FirebaseUser usuario = auth.getUsuarioActual();
        if (usuario == null) {
            abrir(desde, LoginActivity.class, null);
            return;
        }
        if (auth.necesitaVerificarCorreo()) {
            abrir(desde, VerificarCorreoActivity.class, null);
            return;
        }
        new NegocioRepository(desde.getApplication()).resolverInicio(
                usuario.getUid(), usuario.getDisplayName(), destino -> {
                    switch (destino) {
                        case ELEGIR_GIRO:
                            abrir(desde, ElegirGiroActivity.class, null);
                            break;
                        case CATALOGO:
                            abrir(desde, VistaPreviaCatalogoActivity.class, null);
                            break;
                        case ELEGIR_TURNO:
                            abrir(desde, SeleccionarTurnoActivity.class, null);
                            break;
                        case OTRA_CUENTA:
                            auth.cerrarSesion();
                            abrir(desde, LoginActivity.class, desde.getString(R.string.acceso_otra_cuenta));
                            break;
                        default:
                            abrir(desde, PrincipalActivity.class, null);
                    }
                });
    }

    private static void abrir(Activity desde, Class<?> destino, String error) {
        Intent intent = new Intent(desde, destino);
        if (error != null) intent.putExtra(LoginActivity.EXTRA_ERROR, error);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        desde.startActivity(intent);
        desde.finish();
    }
}
