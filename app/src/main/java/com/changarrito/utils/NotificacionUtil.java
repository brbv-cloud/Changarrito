package com.changarrito.utils;

import com.changarrito.R;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.ui.activities.PrincipalActivity;

/**
 * Muestra las notificaciones del sistema para las alertas de stock bajo / proximo
 * vencimiento. En Android 13+ requiere el permiso POST_NOTIFICATIONS: si el usuario
 * no lo concedio, notify() lanza SecurityException, que se captura silenciosamente
 * (la app sigue funcionando, solo no se ve la notificacion del sistema; la alerta
 * de todas formas queda visible en la pestana Alertas).
 */
public class NotificacionUtil {

    private static final String CANAL_ID = "alertas_changarrito";
    private static final String CANAL_NOMBRE = "Alertas de inventario";

    private NotificacionUtil() {
    }

    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription("Avisos de stock bajo y productos por vencer");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }

    public static void mostrarNotificacionAlerta(Context context, int idNotificacion, TipoAlerta tipo,
                                                  String nombreProducto, String mensaje) {
        crearCanal(context);

        // Abre la app directo en la pestana Alertas (si ya estaba abierta, la reutiliza).
        Intent intent = new Intent(context, PrincipalActivity.class)
                .putExtra(PrincipalActivity.EXTRA_SECCION, PrincipalActivity.SECCION_ALERTAS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, idNotificacion, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String titulo = tipo == TipoAlerta.AGOTADO ? "Se acabó"
                : tipo == TipoAlerta.STOCK_BAJO ? "Stock bajo" : "Próximo a vencer";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_stat_changarrito)
                .setContentTitle(titulo + ": " + nombreProducto)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(idNotificacion, builder.build());
        } catch (SecurityException e) {
            // Permiso POST_NOTIFICATIONS no concedido: seguimos sin romper la app.
        }
    }

    // ------------------------------------------------------------------ respaldo

    private static final String CANAL_RESPALDO = "respaldo_changarrito";
    /** Una sola notificacion de respaldo a la vez: "falta respaldar" se cambia por "listo". */
    public static final int ID_RESPALDO = 7001;

    /** 9 pm sin internet: la informacion de hoy todavia no esta en la nube. */
    public static void mostrarRespaldoPendiente(Context c) {
        notificarRespaldo(c, false);
    }

    /** Se termino el respaldo que pidio el recordatorio de la noche. */
    public static void mostrarRespaldoListo(Context c) {
        notificarRespaldo(c, true);
    }

    /** Al respaldar (solo o a mano), el aviso de "falta respaldar" ya no aplica. */
    public static void quitarRespaldoPendiente(Context c) {
        NotificationManagerCompat.from(c).cancel(ID_RESPALDO);
    }

    private static void notificarRespaldo(Context c, boolean listo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_RESPALDO,
                    c.getString(R.string.noti_respaldo_canal), NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription(c.getString(R.string.noti_respaldo_canal_desc));
            NotificationManager manager = c.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }
        Intent intent = new Intent(c, PrincipalActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent abrir = PendingIntent.getActivity(c, ID_RESPALDO, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String texto = c.getString(listo ? R.string.noti_respaldo_listo : R.string.noti_respaldo_pendiente);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CANAL_RESPALDO)
                .setSmallIcon(R.drawable.ic_stat_changarrito)
                .setContentTitle(c.getString(listo ? R.string.noti_respaldo_listo_t : R.string.noti_respaldo_pendiente_t))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSilent(listo) // el "listo" no suena: solo queda en la barra
                .setContentIntent(abrir)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(ID_RESPALDO, b.build());
        } catch (SecurityException e) {
            // Sin permiso de notificaciones: el respaldo sigue funcionando igual.
        }
    }
}
