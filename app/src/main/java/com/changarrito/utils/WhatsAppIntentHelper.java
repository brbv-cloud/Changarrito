package com.changarrito.utils;

import com.changarrito.database.entity.UnidadMedida;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Construye el mensaje y la URL de wa.me para el boton "Contactar proveedor".
 * Separado de Context/Intent a proposito, para poder probarlo con un test unitario
 * de JVM normal (sin emulador).
 */
public class WhatsAppIntentHelper {

    private WhatsAppIntentHelper() {
    }

    public static String construirMensaje(String nombreProducto, double cantidadDisponible, UnidadMedida unidad) {
        String unidadTexto = unidad != null ? unidad.name() : "";
        return "Hola, requiero surtir " + nombreProducto
                + ", stock actual: " + formatearCantidad(cantidadDisponible) + " " + unidadTexto;
    }

    public static String construirUrl(String telefono, String mensaje) {
        String telefonoLimpio = telefono != null ? telefono.trim() : "";
        try {
            return "https://wa.me/" + telefonoLimpio + "?text=" + URLEncoder.encode(mensaje, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 siempre esta disponible en la JVM/Android; esto no deberia pasar nunca.
            return "https://wa.me/" + telefonoLimpio;
        }
    }

    private static String formatearCantidad(double cantidad) {
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.getDefault(), "%.0f", cantidad);
        }
        return String.format(Locale.getDefault(), "%.3f", cantidad)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
