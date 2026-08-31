package com.changarrito.scanner;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consulta el nombre de un producto por su código de barras en Open Food Facts.
 * Es un enriquecimiento OPCIONAL: si no hay internet o el producto no existe,
 * devuelve null y el usuario captura los datos a mano.
 * El escaneo en sí nunca depende de la red.
 */
public class OpenFoodFactsClient {

    private static final String TAG = "OpenFoodFacts";
    private static final String BASE_URL =
            "https://world.openfoodfacts.org/api/v2/product/%s.json?fields=product_name,brands,quantity";
    private static final int TIMEOUT_MS = 5000;

    public interface Callback {
        /** nombre puede ser null si no se encontró o no hubo internet. */
        void onResultado(String nombre);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void buscarNombre(String barcode, Callback callback) {
        executor.execute(() -> {
            String nombre = consultar(barcode);
            mainHandler.post(() -> callback.onResultado(nombre));
        });
    }

    private static String consultar(String barcode) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(String.format(BASE_URL, barcode));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Changarrito/1.0 (Android)");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    sb.append(linea);
                }
            }

            JSONObject json = new JSONObject(sb.toString());
            if (json.optInt("status", 0) != 1) {
                return null;
            }

            JSONObject producto = json.optJSONObject("product");
            if (producto == null) return null;

            String nombre = producto.optString("product_name", "").trim();
            String marca = producto.optString("brands", "").trim();
            String cantidad = producto.optString("quantity", "").trim();

            if (nombre.isEmpty()) return null;

            return armarNombre(marca, nombre, cantidad);

        } catch (Exception e) {
            Log.w(TAG, "No se pudo consultar el producto: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Arma "Marca Nombre Tamaño" evitando repetir información que ya venga
     * dentro del nombre (ej. "Coca cola clásica 500ml" + "500 ml").
     */
    static String armarNombre(String marca, String nombre, String cantidad) {
        StringBuilder sb = new StringBuilder();

        if (!marca.isEmpty()) {
            String primeraMarca = marca.split(",")[0].trim();
            if (!contieneTexto(nombre, primeraMarca)) {
                sb.append(primeraMarca).append(" ");
            }
        }

        sb.append(nombre);

        if (!cantidad.isEmpty() && !contieneTexto(nombre, cantidad)) {
            sb.append(" ").append(cantidad);
        }

        return sb.toString().trim();
    }

    /**
     * Compara ignorando mayúsculas, espacios y acentos ligeros,
     * para que "500ml" y "500 ml" cuenten como lo mismo.
     */
    private static boolean contieneTexto(String texto, String fragmento) {
        String a = normalizar(texto);
        String b = normalizar(fragmento);
        return !b.isEmpty() && a.contains(b);
    }

    private static String normalizar(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s.,]", "");
    }
}