package com.changarrito.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.ui.activities.AlertasActivity;

/**
 * Muestra las notificaciones del sistema para las alertas de stock bajo / proximo
 * vencimiento. En Android 13+ requiere el permiso POST_NOTIFICATIONS: si el usuario
 * no lo concedio, notify() lanza SecurityException, que se captura silenciosamente
 * (la app sigue funcionando, solo no se ve la notificacion del sistema; la alerta
 * de todas formas queda visible en AlertasActivity).
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

        Intent intent = new Intent(context, AlertasActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, idNotificacion, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String titulo = tipo == TipoAlerta.STOCK_BAJO ? "Stock bajo" : "Proximo a vencer";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
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
}
