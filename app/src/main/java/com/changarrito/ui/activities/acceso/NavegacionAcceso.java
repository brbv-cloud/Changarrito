package com.changarrito.ui.activities.acceso;

import android.app.Activity;
import android.content.Intent;

import com.changarrito.auth.AuthRepository;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.ui.activities.PrincipalActivity;
import com.changarrito.ui.activities.negocio.ElegirGiroActivity;
import com.changarrito.ui.activities.negocio.SeleccionarTurnoActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * Decide a qué pantalla va el usuario:
 *  - sin sesión → Login
 *  - correo sin verificar → Verificar correo
 *  - primera vez → Elige tu giro → (vista previa del catálogo)
 *  - el celular ya tiene el negocio de otra cuenta → Cambiar de negocio
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
                            // Dejo la configuracion a medias (cerro la app en el catalogo): se
                            // regresa a "Elige tu giro" con lo que ya habia puesto, para que lo
                            // confirme o lo cambie, en vez de seguir con un giro que no confirmo.
                            abrir(desde, ElegirGiroActivity.class, null);
                            break;
                        case ELEGIR_TURNO:
                            abrir(desde, SeleccionarTurnoActivity.class, null);
                            break;
                        case OTRA_CUENTA:
                            // El celular tiene otro negocio: se explica y se ofrece dejarlo libre.
                            abrir(desde, CambiarNegocioActivity.class, null);
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
