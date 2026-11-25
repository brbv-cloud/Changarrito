package com.changarrito.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración del negocio de este celular. Siempre hay como máximo UNA fila
 * (id = ID_UNICO). Se respalda en Firestore junto con lo demás, así que al
 * reinstalar o cambiar de celular el dueño recupera su giro y sus ajustes.
 */
@Entity(tableName = "configuracion_negocio")
public class ConfiguracionNegocioEntity {
    public static final int ID_UNICO = 1;

    @PrimaryKey
    public int id = ID_UNICO;

    /** uid de Firebase Auth del dueño: también es la carpeta de su respaldo, negocios/{uid}. */
    public String uidFirebase;
    public String nombreNegocio;
    public GiroNegocio giro;
    /** Unidades habilitadas separadas por coma, ej. "PZA,KG,GR". Ver getUnidades(). */
    public String unidadesHabilitadas;
    public ModoCaducidad modoCaducidad;
    /** % de ganancia sobre el costo que se propone al dar de alta un producto. */
    public double margenPorDefecto;
    public boolean catalogoImportado;
    public long timestampActualizacionMs;

    /** Configuración inicial para un giro recién elegido. */
    public static ConfiguracionNegocioEntity paraGiro(GiroNegocio giro, String uidFirebase,
                                                      double margenPorDefecto) {
        ConfiguracionNegocioEntity c = new ConfiguracionNegocioEntity();
        c.uidFirebase = uidFirebase;
        c.giro = giro;
        c.unidadesHabilitadas = unirUnidades(giro.unidadesPorDefecto);
        c.modoCaducidad = giro.caducidadPorDefecto;
        c.margenPorDefecto = margenPorDefecto;
        c.catalogoImportado = false;
        c.timestampActualizacionMs = System.currentTimeMillis();
        return c;
    }

    public List<UnidadMedida> getUnidades() {
        List<UnidadMedida> unidades = new ArrayList<>();
        if (unidadesHabilitadas == null || unidadesHabilitadas.trim().isEmpty()) {
            unidades.add(UnidadMedida.PZA);
            return unidades;
        }
        for (String parte : unidadesHabilitadas.split(",")) {
            try {
                unidades.add(UnidadMedida.valueOf(parte.trim()));
            } catch (IllegalArgumentException ignorada) {
                // Unidad que ya no existe (ej. "LT" de una versión vieja): se omite.
            }
        }
        if (unidades.isEmpty()) {
            unidades.add(UnidadMedida.PZA);
        }
        return unidades;
    }

    public static String unirUnidades(List<UnidadMedida> unidades) {
        StringBuilder sb = new StringBuilder();
        for (UnidadMedida u : unidades) {
            if (sb.length() > 0) sb.append(',');
            sb.append(u.name());
        }
        return sb.toString();
    }
}
