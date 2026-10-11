package com.changarrito.negocio;

import android.content.Context;
import android.content.SharedPreferences;

import com.changarrito.database.entity.Rol;

/**
 * Quién está atendiendo ahora en este celular (dueño o trabajador). Se guarda en
 * SharedPreferences para que el turno siga abierto aunque se cierre la app, hasta
 * que alguien toque "Cambiar de turno" o se cierre la sesión.
 *
 * Las ventas y los cierres de caja guardan este id (idUsuario), y las pantallas lo
 * usan para saber qué mostrar según el rol.
 */
public final class SesionTurno {

    private static final String PREFS = "sesion_turno";
    private static final String CLAVE_ID = "id_usuario";
    private static final String CLAVE_NOMBRE = "nombre";
    private static final String CLAVE_ROL = "rol";

    private SesionTurno() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void abrir(Context c, int idUsuario, String nombre, Rol rol) {
        prefs(c).edit()
                .putInt(CLAVE_ID, idUsuario)
                .putString(CLAVE_NOMBRE, nombre)
                .putString(CLAVE_ROL, rol.name())
                .apply();
    }

    public static void cerrar(Context c) {
        prefs(c).edit().clear().apply();
    }

    /** null si no hay turno abierto. */
    public static Integer getIdUsuario(Context c) {
        int id = prefs(c).getInt(CLAVE_ID, -1);
        return id > 0 ? id : null;
    }

    public static String getNombre(Context c) {
        return prefs(c).getString(CLAVE_NOMBRE, "");
    }

    /** Sin turno se trata como dueño: es el caso de un negocio sin trabajadores. */
    public static boolean esDueno(Context c) {
        return !Rol.TRABAJADOR.name().equals(prefs(c).getString(CLAVE_ROL, Rol.DUENO.name()));
    }
}
