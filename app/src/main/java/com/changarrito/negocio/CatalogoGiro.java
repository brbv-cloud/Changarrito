package com.changarrito.negocio;

import android.content.Context;
import android.util.Log;

import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.utils.CalculoPrecio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo sugerido de un giro (assets/catalogo_*.json), agrupado por categoría en
 * el orden del archivo. Lo usa la vista previa para que el dueño elija qué importar.
 * Los productos se importan con existencia 0: el comerciante captura su stock real.
 */
public class CatalogoGiro {

    private static final String TAG = "CatalogoGiro";

    public final double margenSugerido;
    public final Map<String, List<ProductoEntity>> porCategoria;

    private CatalogoGiro(double margenSugerido, Map<String, List<ProductoEntity>> porCategoria) {
        this.margenSugerido = margenSugerido;
        this.porCategoria = porCategoria;
    }

    public int total() {
        int n = 0;
        for (List<ProductoEntity> l : porCategoria.values()) n += l.size();
        return n;
    }

    /** Vacío (sin categorías) si el giro no tiene catálogo o el archivo no se pudo leer. */
    public static CatalogoGiro cargar(Context context, GiroNegocio giro) {
        Map<String, List<ProductoEntity>> grupos = new LinkedHashMap<>();
        double margen = 30;
        if (giro.archivoCatalogo == null) return new CatalogoGiro(margen, grupos);
        try {
            JSONObject raiz = new JSONObject(leer(context, giro.archivoCatalogo));
            margen = raiz.optDouble("margenSugerido", margen);
            JSONArray productos = raiz.getJSONArray("productos");
            long ahora = System.currentTimeMillis();
            for (int i = 0; i < productos.length(); i++) {
                JSONObject o = productos.getJSONObject(i);
                ProductoEntity p = new ProductoEntity();
                p.nombre = o.getString("nombre");
                p.categoria = o.getString("categoria");
                p.unidadMedida = UnidadMedida.valueOf(o.getString("unidadMedida"));
                p.costoCompra = o.getDouble("costoCompra");
                p.precio = o.getDouble("precio");
                p.margenPorcentaje = CalculoPrecio.redondearCentavos(
                        CalculoPrecio.margenSobreCosto(p.costoCompra, p.precio));
                p.umbralStockBajo = o.optDouble("umbralStockBajo", 0);
                p.cantidadDisponible = 0;
                p.telefonoProveedor = "";
                p.barcode = "";
                p.activo = true;
                p.timestampCreacionMs = ahora;
                p.timestampActualizacionMs = ahora;
                List<ProductoEntity> lista = grupos.get(p.categoria);
                if (lista == null) {
                    lista = new ArrayList<>();
                    grupos.put(p.categoria, lista);
                }
                lista.add(p);
            }
        } catch (Exception e) {
            Log.e(TAG, "No se pudo leer " + giro.archivoCatalogo, e);
            grupos.clear();
        }
        return new CatalogoGiro(margen, grupos);
    }

    private static String leer(Context context, String archivo) throws Exception {
        try (InputStream in = context.getAssets().open(archivo);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
