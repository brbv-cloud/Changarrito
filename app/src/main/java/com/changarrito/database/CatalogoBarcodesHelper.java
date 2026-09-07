package com.changarrito.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Consulta el catálogo offline de códigos de barras (dataset de Open Food Facts / Open Beauty
 * Facts, ~15,000 productos comunes) que viaja empaquetado en assets/catalogo_barcodes.db.
 *
 * Es 100% local: la primera vez que se usa, copia el archivo de assets al almacenamiento
 * interno de la app, porque Android no puede abrir una base de datos directamente dentro de
 * assets/ (solo la puede leer como stream). Después de esa copia inicial, las consultas son
 * instantáneas y no requieren internet.
 *
 * No sustituye a OpenFoodFactsClient (la consulta en línea): es más rápido y funciona sin
 * conexión, pero el dataset es un snapshot fijo tomado al armar la app, no se actualiza solo.
 * El orden recomendado al escanear es: 1) inventario local (Room) 2) este catálogo offline
 * 3) Open Food Facts en línea como último recurso.
 */
public class CatalogoBarcodesHelper {

    private static final String TAG = "CatalogoBarcodes";
    private static final String NOMBRE_DB = "catalogo_barcodes.db";

    private static volatile CatalogoBarcodesHelper instancia;

    private final SQLiteDatabase db;

    private CatalogoBarcodesHelper(Context context) {
        db = abrirBaseDatos(context.getApplicationContext());
    }

    public static CatalogoBarcodesHelper getInstance(Context context) {
        if (instancia == null) {
            synchronized (CatalogoBarcodesHelper.class) {
                if (instancia == null) {
                    instancia = new CatalogoBarcodesHelper(context);
                }
            }
        }
        return instancia;
    }

    /**
     * Busca el nombre de un producto por su código de barras.
     * ⚠️ Debe llamarse en un hilo de fondo: hace I/O de disco (y, en el primer uso, copia
     * el archivo completo desde assets). Nunca invocar desde el hilo principal.
     *
     * @return el nombre del producto, o null si no está en el catálogo o si la BD no
     *         se pudo abrir (ej. dispositivo con poco almacenamiento).
     */
    public String buscarNombre(String barcode) {
        if (db == null || barcode == null || barcode.isEmpty()) return null;

        try (Cursor cursor = db.rawQuery(
                "SELECT nombre FROM catalogo_barcodes WHERE barcode = ? LIMIT 1",
                new String[]{barcode})) {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error consultando catálogo offline: " + e.getMessage());
        }
        return null;
    }

    private SQLiteDatabase abrirBaseDatos(Context context) {
        File destino = context.getDatabasePath(NOMBRE_DB);

        try {
            if (!destino.exists()) {
                File carpeta = destino.getParentFile();
                if (carpeta != null && !carpeta.exists()) {
                    carpeta.mkdirs();
                }
                copiarDesdeAssets(context, destino);
            }
            return SQLiteDatabase.openDatabase(
                    destino.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (IOException e) {
            Log.e(TAG, "No se pudo copiar el catálogo offline desde assets: " + e.getMessage());
            return null;
        }
    }

    private void copiarDesdeAssets(Context context, File destino) throws IOException {
        try (InputStream in = context.getAssets().open(NOMBRE_DB);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = in.read(buffer)) > 0) {
                out.write(buffer, 0, leidos);
            }
            out.flush();
        }
    }
}
