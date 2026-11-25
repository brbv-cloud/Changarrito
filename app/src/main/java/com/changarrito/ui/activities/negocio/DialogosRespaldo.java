package com.changarrito.ui.activities.negocio;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.respaldo.RespaldoNube;
import com.changarrito.respaldo.RespaldoRepository;
import com.changarrito.ui.activities.acceso.NavegacionAcceso;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Dialogos y textos del respaldo en la nube, compartidos por Ajustes y Elegir giro. */
public final class DialogosRespaldo {

    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private DialogosRespaldo() {
    }

    /** "hoy a las 22:10", "ayer a las 09:30" o "24/10/2026 a las 18:00". */
    public static String cuando(Context c, long ms) {
        String hora = new SimpleDateFormat("HH:mm", MX).format(new Date(ms));
        Calendar hoy = Calendar.getInstance();
        Calendar dia = Calendar.getInstance();
        dia.setTimeInMillis(ms);
        if (mismoDia(hoy, dia)) return c.getString(R.string.respaldo_hoy, hora);
        hoy.add(Calendar.DAY_OF_YEAR, -1);
        if (mismoDia(hoy, dia)) return c.getString(R.string.respaldo_ayer, hora);
        return c.getString(R.string.respaldo_fecha, new SimpleDateFormat("dd/MM/yyyy", MX).format(new Date(ms)), hora);
    }

    private static boolean mismoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String fecha(long ms) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", MX).format(new Date(ms));
    }

    private static String nombre(RespaldoNube.InfoRespaldo info) {
        return info.nombreNegocio != null ? info.nombreNegocio : "tu negocio";
    }

    private static boolean viva(AppCompatActivity a) {
        return !a.isFinishing() && !a.isDestroyed();
    }

    /** Desde Ajustes: confirma que se va a reemplazar todo y restaura. */
    public static void confirmarReemplazo(AppCompatActivity a, RespaldoNube.InfoRespaldo info) {
        if (!viva(a)) return;
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.respaldo_confirmar_titulo)
                .setMessage(a.getString(R.string.respaldo_confirmar_texto, nombre(info), fecha(info.fechaMs),
                        info.dispositivo != null ? info.dispositivo : "?",
                        RespaldoRepository.resumen(a, info)))
                .setPositiveButton(R.string.respaldo_confirmar_si, (d, w) -> restaurar(a))
                .setNegativeButton(R.string.respaldo_cancelar, null)
                .show();
    }

    /** Celular nuevo: ofrece recuperar el respaldo antes de configurar desde cero. */
    public static void ofrecerRecuperar(AppCompatActivity a, RespaldoNube.InfoRespaldo info) {
        if (!viva(a)) return;
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.respaldo_encontrado_titulo)
                .setMessage(a.getString(R.string.respaldo_encontrado_texto, nombre(info), fecha(info.fechaMs),
                        RespaldoRepository.resumen(a, info)))
                .setCancelable(false)
                .setPositiveButton(R.string.respaldo_confirmar_si, (d, w) -> restaurar(a))
                .setNegativeButton(R.string.respaldo_empezar_cero, (d, w) ->
                        Toast.makeText(a, R.string.respaldo_empezar_cero_aviso, Toast.LENGTH_LONG).show())
                .show();
    }

    /** Restaura con un aviso de progreso y, al terminar, manda a la pantalla que toque. */
    public static void restaurar(AppCompatActivity a) {
        AlertDialog progreso = new MaterialAlertDialogBuilder(a)
                .setMessage(R.string.respaldo_restaurando)
                .setCancelable(false)
                .show();
        new RespaldoRepository(a.getApplication()).restaurar(new RespaldoRepository.Callback<RespaldoNube.InfoRespaldo>() {
            @Override
            public void onExito(RespaldoNube.InfoRespaldo info) {
                if (!viva(a)) return;
                progreso.dismiss();
                Toast.makeText(a, R.string.respaldo_restaurado, Toast.LENGTH_LONG).show();
                NavegacionAcceso.irAlDestino(a, new AuthRepository());
            }

            @Override
            public void onError(String mensaje) {
                if (!viva(a)) return;
                progreso.dismiss();
                mostrarError(a, mensaje);
            }
        });
    }

    public static void mostrarError(AppCompatActivity a, String mensaje) {
        if (!viva(a)) return;
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.respaldo_seccion)
                .setMessage(mensaje)
                .setPositiveButton(R.string.legal_entendido, null)
                .show();
    }
}
