package com.changarrito.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Todo lo que la app hace con Firebase Authentication pasa por aquí.
 *
 * Offline-first: Firebase guarda la sesión en el celular, así que después del primer
 * inicio de sesión la app entra sin internet (getUsuarioActual() no necesita red).
 * Solo crear cuenta, iniciar sesión, verificar el correo y recuperar contraseña la necesitan.
 *
 * Las contraseñas nunca pasan por nuestra base de datos: se las damos directo a
 * Firebase, que las guarda cifradas en sus servidores.
 *
 * Los callbacks llegan en el hilo principal (así trabaja la Task de Firebase), por lo
 * que se puede tocar la interfaz directamente desde ellos.
 */
public class AuthRepository {

    public interface Callback {
        void onExito();

        void onError(String mensaje);
    }

    private final FirebaseAuth auth;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        // Los correos de verificación y de recuperación salen en el idioma del celular.
        auth.useAppLanguage();
    }

    @Nullable
    public FirebaseUser getUsuarioActual() {
        return auth.getCurrentUser();
    }

    /**
     * true si quien inició sesión con correo y contraseña todavía no abre el enlace de
     * verificación. Las cuentas de Google llegan ya verificadas.
     */
    public boolean necesitaVerificarCorreo() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null && !u.isEmailVerified();
    }

    public void iniciarSesion(String correo, String contrasena, Callback callback) {
        escuchar(auth.signInWithEmailAndPassword(correo.trim(), contrasena), callback);
    }

    /** Crea la cuenta, le pone el nombre y manda el correo de verificación. */
    public void registrar(String nombre, String correo, String contrasena, Callback callback) {
        auth.createUserWithEmailAndPassword(correo.trim(), contrasena)
                .addOnSuccessListener(resultado -> {
                    FirebaseUser usuario = resultado.getUser();
                    if (usuario == null) {
                        callback.onError(MensajesErrorAuth.GENERICO);
                        return;
                    }
                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre.trim())
                            .build();
                    // Si el nombre o el correo fallan, la cuenta ya existe: seguimos y el
                    // usuario puede pedir que se reenvíe el correo desde la pantalla de verificación.
                    usuario.updateProfile(perfil)
                            .continueWithTask(t -> usuario.sendEmailVerification())
                            .addOnCompleteListener(t -> callback.onExito());
                })
                .addOnFailureListener(e -> callback.onError(traducir(e)));
    }

    public void entrarConGoogle(String idToken, Callback callback) {
        AuthCredential credencial = GoogleAuthProvider.getCredential(idToken, null);
        escuchar(auth.signInWithCredential(credencial), callback);
    }

    /**
     * Firebase no dice si el correo existe (protección contra enumeración), así que
     * la pantalla siempre muestra el mismo mensaje de "si hay una cuenta, te llegará".
     */
    public void enviarRecuperacion(String correo, Callback callback) {
        escuchar(auth.sendPasswordResetEmail(correo.trim()), callback);
    }

    public void reenviarVerificacion(Callback callback) {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            callback.onError(MensajesErrorAuth.GENERICO);
            return;
        }
        escuchar(u.sendEmailVerification(), callback);
    }

    /**
     * Vuelve a preguntarle a Firebase si el correo ya se verificó. Si sí, también
     * renueva el token: las reglas de Firestore leen email_verified del token, y el
     * viejo todavía dice false.
     */
    public void comprobarVerificacion(Callback callback) {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            callback.onError(MensajesErrorAuth.GENERICO);
            return;
        }
        u.reload()
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    FirebaseUser actualizado = auth.getCurrentUser();
                    if (actualizado == null || !actualizado.isEmailVerified()) {
                        throw new CorreoNoVerificadoException();
                    }
                    return actualizado.getIdToken(true);
                })
                .addOnSuccessListener(r -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(
                        e instanceof CorreoNoVerificadoException ? null : traducir(e)));
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    private void escuchar(Task<?> tarea, Callback callback) {
        tarea.addOnSuccessListener(r -> callback.onExito())
                .addOnFailureListener(e -> callback.onError(traducir(e)));
    }

    static String traducir(@NonNull Exception e) {
        if (e instanceof FirebaseNetworkException) return MensajesErrorAuth.SIN_CONEXION;
        if (e instanceof FirebaseTooManyRequestsException) return MensajesErrorAuth.DEMASIADOS_INTENTOS;
        if (e instanceof FirebaseAuthException) {
            return MensajesErrorAuth.porCodigo(((FirebaseAuthException) e).getErrorCode());
        }
        return MensajesErrorAuth.GENERICO;
    }

    /** Marca interna: el correo sigue sin verificarse (no es un error de red ni de Firebase). */
    private static class CorreoNoVerificadoException extends Exception {
    }
}
