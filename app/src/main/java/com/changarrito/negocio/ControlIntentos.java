package com.changarrito.negocio;

/**
 * Frena a quien intente adivinar la clave de 4 dígitos de un perfil probando
 * combinaciones: después de MAX_INTENTOS fallos seguidos, el perfil queda bloqueado
 * BLOQUEO_MS. Clase pura (sin Android) para probarla con JUnit.
 */
public final class ControlIntentos {

    public static final int MAX_INTENTOS = 5;
    public static final long BLOQUEO_MS = 5 * 60_000L;

    private ControlIntentos() {
    }

    /** Milisegundos que faltan de bloqueo; 0 si no está bloqueado. */
    public static long msRestantes(long bloqueadoHastaMs, long ahoraMs) {
        return Math.max(0, bloqueadoHastaMs - ahoraMs);
    }

    /** Estado después de un intento fallido: {intentosFallidos, bloqueadoHastaMs}. */
    public static long[] registrarFallo(int intentosFallidos, long ahoraMs) {
        int intentos = intentosFallidos + 1;
        if (intentos >= MAX_INTENTOS) {
            return new long[]{0, ahoraMs + BLOQUEO_MS};
        }
        return new long[]{intentos, 0};
    }

    public static int intentosRestantes(int intentosFallidos) {
        return Math.max(0, MAX_INTENTOS - intentosFallidos);
    }
}
