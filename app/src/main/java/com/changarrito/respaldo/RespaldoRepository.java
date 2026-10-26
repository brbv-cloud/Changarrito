package com.changarrito.respaldo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.changarrito.R;
import com.changarrito.database.AppDatabase;
import com.changarrito.negocio.SesionTurno;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Respaldo en la nube del negocio: respaldar, consultar y restaurar.
 *
 * Los metodos *Sync corren en el hilo que los llama (el worker ya esta en fondo).
 * Los demas corren en un hilo propio y devuelven el resultado en el hilo principal.
 * Un candado global evita que el respaldo automatico y el manual corran a la vez.
 */
public class RespaldoRepository {

    public interface Callback<T> {
        void onExito(T resultado);

        void onError(String mensaje);
    }

    private static final String TAG = "Respaldo";
    private static final Object CANDADO = new Object();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREFS = "respaldo_nube";
    private static final String CLAVE_ULTIMO_MS = "ultimo_ms";
    private static final String CLAVE_ULTIMO_RESUMEN = "ultimo_resumen";

    private final Application app;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public RespaldoRepository(Application app) {
        this.app = app;
    }

    // ---------------------------------------------------------------- sincronos

    public RespaldoNube.InfoRespaldo respaldarSync() throws RespaldoException {
        synchronized (CANDADO) {
            Log.i(TAG, "Inicio respaldo");
            FirebaseUser cuenta = cuentaValida();
            exigirInternet();
            Log.i(TAG, "Leyendo base de datos local");
            RespaldoCompleto foto = RespaldoLocal.leer(AppDatabase.getInstance(app));
            Log.i(TAG, "Leidos: " + foto.productos.size() + " productos, " + foto.ventas.size() + " ventas, "
                    + foto.usuarios.size() + " usuarios, " + foto.cierres.size() + " cierres, "
                    + foto.bitacora.size() + " bitacora");
            if (foto.estaVacio()) {
                // Todavia no elige giro: no hay negocio que respaldar (y no se pisa uno bueno).
                throw new RespaldoException(RespaldoException.Tipo.SIN_RESPALDO, null);
            }
            RespaldoNube.InfoRespaldo info = conReintentoDeToken(cuenta,
                    () -> new RespaldoNube().subir(cuenta.getUid(), foto, nombreDispositivo()));
            guardarUltimo(info);
            return info;
        }
    }

    /** null si la cuenta no tiene respaldo en la nube. */
    public RespaldoNube.InfoRespaldo consultarSync() throws RespaldoException {
        FirebaseUser cuenta = cuentaValida();
        exigirInternet();
        return conReintentoDeToken(cuenta, () -> new RespaldoNube().consultar(cuenta.getUid()));
    }

    public RespaldoNube.InfoRespaldo restaurarSync() throws RespaldoException {
        synchronized (CANDADO) {
            FirebaseUser cuenta = cuentaValida();
            exigirInternet();
            RespaldoNube nube = new RespaldoNube();
            RespaldoNube.InfoRespaldo info = conReintentoDeToken(cuenta, () -> nube.consultar(cuenta.getUid()));
            if (info == null) throw new RespaldoException(RespaldoException.Tipo.SIN_RESPALDO, null);
            RespaldoCompleto foto = conReintentoDeToken(cuenta, () -> nube.bajar(cuenta.getUid()));
            if (foto == null) throw new RespaldoException(RespaldoException.Tipo.SIN_RESPALDO, null);

            RespaldoLocal.reemplazar(AppDatabase.getInstance(app), foto, cuenta.getUid());
            // Los perfiles pudieron cambiar: que vuelvan a elegir quien atiende.
            SesionTurno.cerrar(app);
            guardarUltimo(info);
            return info;
        }
    }

    // ---------------------------------------------------------------- asincronos

    public void respaldar(Callback<RespaldoNube.InfoRespaldo> callback) {
        enFondo(this::respaldarSync, callback);
    }

    public void consultar(Callback<RespaldoNube.InfoRespaldo> callback) {
        enFondo(this::consultarSync, callback);
    }

    public void restaurar(Callback<RespaldoNube.InfoRespaldo> callback) {
        enFondo(this::restaurarSync, callback);
    }

    private interface Trabajo<T> {
        T hacer() throws RespaldoException;
    }

    private <T> void enFondo(Trabajo<T> trabajo, Callback<T> callback) {
        EXECUTOR.execute(() -> {
            try {
                T r = trabajo.hacer();
                principal.post(() -> callback.onExito(r));
            } catch (RespaldoException e) {
                Log.e(TAG, "Fallo el respaldo: " + e.tipo, e);
                String msg = mensaje(app, e.tipo) + detalle(e);
                principal.post(() -> callback.onError(msg));
            } catch (Throwable e) {
                // Throwable y no solo Exception: un error raro nunca debe cerrar la app.
                Log.e(TAG, "Error inesperado en el respaldo", e);
                String msg = mensaje(app, RespaldoException.Tipo.DESCONOCIDO) + detalle(e);
                principal.post(() -> callback.onError(msg));
            }
        });
    }

    /**
     * Detalle tecnico (clase y mensaje de la causa) para los errores desconocidos,
     * asi una captura de pantalla basta para saber que paso.
     */
    private static String detalle(Throwable e) {
        if (e instanceof RespaldoException && ((RespaldoException) e).tipo != RespaldoException.Tipo.DESCONOCIDO) {
            return "";
        }
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) raiz = raiz.getCause();
        String m = raiz.getMessage() != null ? raiz.getMessage() : "";
        if (m.length() > 240) m = m.substring(0, 240) + "…";
        return "\n\n(" + raiz.getClass().getSimpleName() + (m.isEmpty() ? "" : ": " + m) + ")";
    }

    // ---------------------------------------------------------------- estado local

    /** Fecha del ultimo respaldo hecho o restaurado desde este celular (0 = nunca). */
    public long getUltimoRespaldoMs() {
        return prefs().getLong(CLAVE_ULTIMO_MS, 0);
    }

    /** "80 productos · 230 ventas" del ultimo respaldo, o null. */
    public String getUltimoResumen() {
        return prefs().getString(CLAVE_ULTIMO_RESUMEN, null);
    }

    private void guardarUltimo(RespaldoNube.InfoRespaldo info) {
        prefs().edit()
                .putLong(CLAVE_ULTIMO_MS, info.fechaMs)
                .putString(CLAVE_ULTIMO_RESUMEN, resumen(app, info))
                .apply();
    }

    private SharedPreferences prefs() {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String resumen(Context c, RespaldoNube.InfoRespaldo info) {
        return c.getResources().getQuantityString(R.plurals.respaldo_productos, (int) info.productos, (int) info.productos)
                + " · " + c.getResources().getQuantityString(R.plurals.respaldo_ventas, (int) info.ventas, (int) info.ventas);
    }

    // ---------------------------------------------------------------- validaciones

    private FirebaseUser cuentaValida() throws RespaldoException {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || !u.isEmailVerified()) {
            throw new RespaldoException(RespaldoException.Tipo.SIN_SESION, null);
        }
        return u;
    }

    private void exigirInternet() throws RespaldoException {
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        boolean hay = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (!hay) throw new RespaldoException(RespaldoException.Tipo.SIN_INTERNET, null);
    }

    private interface Llamada<T> {
        T hacer() throws Exception;
    }

    /**
     * Las reglas piden "correo verificado". Justo despues de verificar el correo, la
     * sesion puede traer todavia la marca vieja: si Firestore dice "sin permiso",
     * se refresca la sesion una vez y se reintenta.
     */
    private <T> T conReintentoDeToken(FirebaseUser cuenta, Llamada<T> llamada) throws RespaldoException {
        try {
            return llamada.hacer();
        } catch (Exception e) {
            RespaldoException.Tipo tipo = clasificar(e);
            if (tipo != RespaldoException.Tipo.SIN_PERMISO) throw aRespaldoException(e, tipo);
        }
        try {
            Tasks.await(cuenta.getIdToken(true), 30, TimeUnit.SECONDS);
            return llamada.hacer();
        } catch (Exception e) {
            throw aRespaldoException(e, clasificar(e));
        }
    }

    private static RespaldoException aRespaldoException(Exception e, RespaldoException.Tipo tipo) {
        if (e instanceof RespaldoException) return (RespaldoException) e;
        return new RespaldoException(tipo, e);
    }

    static RespaldoException.Tipo clasificar(Throwable e) {
        if (e instanceof RespaldoException) return ((RespaldoException) e).tipo;
        if (e instanceof TimeoutException) return RespaldoException.Tipo.SIN_INTERNET;
        if (e instanceof ExecutionException && e.getCause() != null) e = e.getCause();
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED:
                case UNAUTHENTICATED:
                    return RespaldoException.Tipo.SIN_PERMISO;
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return RespaldoException.Tipo.SIN_INTERNET;
                default:
                    return RespaldoException.Tipo.DESCONOCIDO;
            }
        }
        return RespaldoException.Tipo.DESCONOCIDO;
    }

    public static String mensaje(Context c, RespaldoException.Tipo tipo) {
        switch (tipo) {
            case SIN_INTERNET:
                return c.getString(R.string.respaldo_error_internet);
            case SIN_SESION:
                return c.getString(R.string.respaldo_error_sesion);
            case SIN_PERMISO:
                return c.getString(R.string.respaldo_error_permiso);
            case SIN_RESPALDO:
                return c.getString(R.string.respaldo_error_sin_respaldo);
            case INCOMPLETO:
                return c.getString(R.string.respaldo_error_incompleto);
            default:
                return c.getString(R.string.respaldo_error_desconocido);
        }
    }

    private static String nombreDispositivo() {
        String marca = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String modelo = Build.MODEL == null ? "" : Build.MODEL;
        if (modelo.toLowerCase(java.util.Locale.ROOT).startsWith(marca.toLowerCase(java.util.Locale.ROOT))) return modelo;
        return (marca.isEmpty() ? "" : Character.toUpperCase(marca.charAt(0)) + marca.substring(1) + " ") + modelo;
    }
}
