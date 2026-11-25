package com.changarrito.auth;

/**
 * Traduce los códigos de error de Firebase Authentication a mensajes en español
 * que el comerciante entienda. Sin dependencias de Firebase para poder probarse con JUnit;
 * AuthRepository le pasa el código (FirebaseAuthException.getErrorCode()) o el tipo de error.
 *
 * Nota: el proyecto tiene activa la "protección contra enumeración de correos" de
 * Firebase, que responde ERROR_INVALID_CREDENTIAL tanto si el correo no existe como
 * si la contraseña está mal. Así nadie puede averiguar qué correos tienen cuenta.
 */
public final class MensajesErrorAuth {

    public static final String SIN_CONEXION =
            "Sin conexión. Para entrar o crear tu cuenta necesitas internet (solo esa vez).";
    public static final String DEMASIADOS_INTENTOS =
            "Demasiados intentos. Espera unos minutos y vuelve a probar.";
    public static final String GENERICO = "Algo salió mal. Vuelve a intentarlo.";

    private MensajesErrorAuth() {
    }

    public static String porCodigo(String codigo) {
        if (codigo == null) return GENERICO;
        switch (codigo) {
            case "ERROR_INVALID_EMAIL":
                return "Ese correo no parece válido.";
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
            case "ERROR_INVALID_LOGIN_CREDENTIALS":
                return "Correo o contraseña incorrectos.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "Ya hay una cuenta con ese correo. Inicia sesión o recupera tu contraseña.";
            case "ERROR_WEAK_PASSWORD":
                return "Esa contraseña es muy fácil de adivinar. Usa al menos 8 caracteres.";
            case "ERROR_USER_DISABLED":
                return "Esta cuenta está desactivada.";
            case "ERROR_TOO_MANY_REQUESTS":
                return DEMASIADOS_INTENTOS;
            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                return "Ese correo ya entró de otra forma. Prueba con correo y contraseña.";
            case "ERROR_NETWORK_REQUEST_FAILED":
                return SIN_CONEXION;
            default:
                return GENERICO;
        }
    }
}
