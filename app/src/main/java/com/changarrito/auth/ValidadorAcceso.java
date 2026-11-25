package com.changarrito.auth;

import java.util.regex.Pattern;

/**
 * Validaciones de los formularios de acceso. Clase pura (sin Android ni Firebase)
 * para probarla con JUnit. Devuelve el mensaje de error a mostrar, o null si es válido.
 */
public final class ValidadorAcceso {

    public static final int MIN_CONTRASENA = 8;

    // Suficiente para atrapar errores de dedo; Firebase hace la validación final.
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private ValidadorAcceso() {
    }

    public static String validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "Escribe tu nombre.";
        if (nombre.trim().length() < 2) return "Escribe tu nombre completo.";
        return null;
    }

    public static String validarCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) return "Escribe tu correo.";
        if (!CORREO.matcher(correo.trim()).matches()) return "Ese correo no parece válido.";
        return null;
    }

    /** Para iniciar sesión solo se pide que no esté vacía (la cuenta pudo crearse con otras reglas). */
    public static String validarContrasenaLogin(String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) return "Escribe tu contraseña.";
        return null;
    }

    public static String validarContrasenaNueva(String contrasena) {
        if (contrasena == null || contrasena.isEmpty()) return "Crea una contraseña.";
        if (contrasena.length() < MIN_CONTRASENA) return "Debe tener al menos " + MIN_CONTRASENA + " caracteres.";
        if (contrasena.trim().isEmpty()) return "No puede ser solo espacios.";
        return null;
    }

    public static String validarConfirmacion(String contrasena, String confirmacion) {
        if (confirmacion == null || confirmacion.isEmpty()) return "Repite tu contraseña.";
        if (!confirmacion.equals(contrasena)) return "Las contraseñas no coinciden.";
        return null;
    }
}
