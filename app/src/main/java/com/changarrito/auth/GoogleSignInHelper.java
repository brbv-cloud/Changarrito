package com.changarrito.auth;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.changarrito.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * "Continuar con Google" con Credential Manager (la forma actual que recomienda
 * Google; la librería vieja de GoogleSignIn está obsoleta).
 *
 * Usa GetSignInWithGoogleOption, la opción pensada para cuando el usuario toca un
 * botón de "Continuar con Google": siempre abre el selector de cuentas de Google
 * (incluida la opción de agregar una), a diferencia de GetGoogleIdOption, que es
 * para la hoja automática y en algunos celulares no muestra nada.
 *
 * Muestra el selector de cuentas de Google del celular, obtiene un ID token y se lo pasa
 * a AuthRepository para entrar a Firebase. Requiere la huella SHA-1 registrada en
 * Firebase; sin ella, Google rechaza la petición.
 */
public class GoogleSignInHelper {

    public interface Callback {
        void onIdToken(String idToken);

        /** mensaje null = el usuario cerró la hoja de cuentas: no hay nada que mostrar. */
        void onError(String mensaje);
    }

    private static final String TAG = "GoogleSignIn";
    /** Si Google no responde en este tiempo, liberamos el botón en lugar de dejarlo congelado. */
    private static final long TIEMPO_MAXIMO_MS = 45_000;

    private final Activity activity;
    private final CredentialManager credentialManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public GoogleSignInHelper(Activity activity) {
        this.activity = activity;
        this.credentialManager = CredentialManager.create(activity);
    }

    public void iniciar(Callback callbackOriginal) {
        // Garantiza que el callback se llame una sola vez (respuesta de Google o tiempo agotado).
        final boolean[] terminado = {false};
        final CancellationSignal cancelacion = new CancellationSignal();
        Callback callback = new Callback() {
            @Override
            public void onIdToken(String idToken) {
                if (terminado[0]) return;
                terminado[0] = true;
                handler.removeCallbacksAndMessages(null);
                callbackOriginal.onIdToken(idToken);
            }

            @Override
            public void onError(String mensaje) {
                if (terminado[0]) return;
                terminado[0] = true;
                handler.removeCallbacksAndMessages(null);
                callbackOriginal.onError(mensaje);
            }
        };
        handler.postDelayed(() -> {
            Log.w(TAG, "Google no respondió en " + TIEMPO_MAXIMO_MS / 1000 + " s");
            cancelacion.cancel();
            callback.onError("Google no respondió. Revisa tu conexión y que el celular tenga Google Play actualizado.");
        }, TIEMPO_MAXIMO_MS);

        GetSignInWithGoogleOption opcionGoogle = new GetSignInWithGoogleOption.Builder(
                // El ID de cliente WEB (no el de Android): lo genera el plugin de google-services.
                activity.getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest peticion = new GetCredentialRequest.Builder()
                .addCredentialOption(opcionGoogle)
                .build();

        Log.d(TAG, "Pidiendo credencial de Google");
        credentialManager.getCredentialAsync(
                activity,
                peticion,
                cancelacion,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse respuesta) {
                        Credential credencial = respuesta.getCredential();
                        Log.d(TAG, "Respuesta recibida: " + credencial.getType());
                        if (credencial instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credencial.getType())) {
                            Bundle datos = ((CustomCredential) credencial).getData();
                            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(datos);
                            callback.onIdToken(google.getIdToken());
                        } else {
                            callback.onError(MensajesErrorAuth.GENERICO);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // Queda en Logcat (filtro "GoogleSignIn") para diagnosticar.
                        Log.e(TAG, "Error de Credential Manager: " + e.getType() + " - " + e.getMessage(), e);
                        if (e instanceof GetCredentialCancellationException) {
                            callback.onError(null);
                        } else if (e instanceof NoCredentialException) {
                            callback.onError("No hay cuentas de Google en este celular. Agrega una en Ajustes o usa tu correo.");
                        } else {
                            callback.onError("No se pudo entrar con Google (" + e.getType() + ").");
                        }
                    }
                });
    }

    /**
     * Al cerrar sesión: le dice a Credential Manager que olvide la cuenta de Google
     * elegida, para que la próxima vez vuelva a mostrar el selector de cuentas.
     */
    public static void olvidarCuentaElegida(Context context) {
        CredentialManager.create(context).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void resultado) {
                    }

                    @Override
                    public void onError(ClearCredentialException e) {
                        Log.w(TAG, "No se pudo limpiar la cuenta elegida", e);
                    }
                });
    }
}
