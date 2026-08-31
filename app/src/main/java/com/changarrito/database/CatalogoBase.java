package com.changarrito.database;

import android.content.Context;
import android.util.Log;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CatalogoBase {

    private static final String TAG = "CatalogoBase";
    private static final String ARCHIVO = "catalogo_base.json";

    /**
     * Lee catalogo_base.json de assets y lo convierte a entidades listas para insertar.
     * Si algo falla, devuelve lista vacía (la app arranca sin catálogo, pero no crashea).
     */
    public static List<ProductoEntity> getProductosIniciales(Context context) {
        List<ProductoEntity> productos = new ArrayList<>();

        try {
            String json = leerArchivo(context);
            JSONArray array = new JSONArray(json);
            long ahora = System.currentTimeMillis();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                ProductoEntity producto = new ProductoEntity();
                producto.nombre = obj.getString("nombre");
                producto.precio = obj.getDouble("precio");
                producto.unidadMedida = UnidadMedida.valueOf(obj.getString("unidadMedida"));
                producto.umbralStockBajo = obj.getDouble("umbralStockBajo");
                producto.cantidadDisponible = 0;      // el comerciante captura su stock real
                producto.fechaCaducidadMs = 0;        // sin fecha por defecto
                producto.telefonoProveedor = "";
                producto.barcode = "";
                producto.timestampCreacionMs = ahora;
                producto.timestampActualizacionMs = ahora;

                productos.add(producto);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar el catálogo base", e);
        }

        return productos;
    }

    private static String leerArchivo(Context context) throws IOException {
        InputStream is = context.getAssets().open(ARCHIVO);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
}