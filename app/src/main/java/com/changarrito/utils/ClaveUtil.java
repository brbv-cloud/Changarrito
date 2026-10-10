package com.changarrito.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Manejo de la clave de 4 dígitos de cada trabajador. La clave nunca se guarda en
 * texto: se guarda el resultado de PBKDF2 (miles de rondas de hash) con una sal
 * aleatoria distinta por usuario. Así, aunque alguien sacara la base de datos del
 * celular, no vería las claves, y dos usuarios con la misma clave tienen hashes distintos.
 *
 * Nota: con solo 10,000 claves posibles, el hash no basta contra quien pruebe todas;
 * por eso UsuarioEntity también lleva intentosFallidos y bloqueadoHastaMs.
 *
 * Se usa PBKDF2WithHmacSHA1 porque es el que existe en todas las versiones desde
 * Android 7.0 (API 24, nuestro mínimo); la variante SHA-256 requiere API 26.
 * Clase pura (sin Android) para probarla con JUnit.
 */
public final class ClaveUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA1";
    private static final int ITERACIONES = 20_000;
    private static final int LONGITUD_BITS = 256;
    private static final int LONGITUD_SAL = 16;

    private ClaveUtil() {
    }

    /** Clave válida = exactamente 4 dígitos. */
    public static boolean esClaveValida(String clave) {
        return clave != null && clave.matches("\\d{4}");
    }

    public static String generarSal() {
        byte[] sal = new byte[LONGITUD_SAL];
        new SecureRandom().nextBytes(sal);
        return aHex(sal);
    }

    public static String hashear(String clave, String salHex) {
        try {
            KeySpec spec = new PBEKeySpec(clave.toCharArray(), deHex(salHex), ITERACIONES, LONGITUD_BITS);
            byte[] hash = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
            return aHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave", e);
        }
    }

    /** Compara en tiempo constante para no filtrar información por cuánto tarda. */
    public static boolean verificar(String clave, String salHex, String hashGuardadoHex) {
        if (clave == null || salHex == null || hashGuardadoHex == null) return false;
        byte[] calculado = deHex(hashear(clave, salHex));
        return MessageDigest.isEqual(calculado, deHex(hashGuardadoHex));
    }

    private static String aHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] deHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
