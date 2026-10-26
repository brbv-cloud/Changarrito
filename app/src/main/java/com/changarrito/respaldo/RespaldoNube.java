package com.changarrito.respaldo;

import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.MermaEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sube y baja la "foto" del negocio en Cloud Firestore. Estructura:
 *
 *   negocios/{uid}                       ← resumen: respaldoActual, fecha, conteos
 *   negocios/{uid}/respaldos/{id}        ← un respaldo (id = fecha en ms)
 *   negocios/{uid}/respaldos/{id}/partes/{tabla_0000}  ← hasta FILAS_POR_PARTE filas
 *
 * Primero se suben TODAS las partes del respaldo nuevo y al final se cambia
 * "respaldoActual" en el resumen. Si el internet se corta a la mitad, el resumen
 * sigue apuntando al respaldo anterior, que esta completo. Despues se borran los
 * respaldos viejos para no gastar espacio.
 *
 * Todo es sincrono (Tasks.await): se llama SIEMPRE desde un hilo de fondo.
 * Las reglas de Firestore (firestore.rules) solo dejan entrar a negocios/{uid}
 * si la sesion es de ese mismo uid y el correo esta verificado.
 */
public class RespaldoNube {

    public static final int VERSION_MODELO = 6;
    static final int FILAS_POR_PARTE = 250;
    /** Firestore acepta hasta 500 operaciones por lote; dejamos margen. */
    static final int OPS_POR_LOTE = 400;
    static final long TIMEOUT_SEGUNDOS = 30;

    static final String T_CONFIG = "configuracion";
    static final String T_USUARIOS = "usuarios";
    static final String T_PRODUCTOS = "productos";
    static final String T_VENTAS = "ventas";
    static final String T_CIERRES = "cierres";
    static final String T_BITACORA = "bitacora";
    static final String T_MERMAS = "mermas";

    /** Resumen del respaldo que hay en la nube (lo que se le muestra al dueno). */
    public static class InfoRespaldo {
        public String id;
        public long fechaMs;
        public String dispositivo;
        public String nombreNegocio;
        public long usuarios, productos, ventas, cierres, bitacora, mermas;
    }

    // Una clase por tabla para que Firestore sepa convertir cada fila a su entidad.
    public static class ParteConfiguracion { public List<ConfiguracionNegocioEntity> filas; }
    public static class ParteUsuarios { public List<UsuarioEntity> filas; }
    public static class ParteProductos { public List<ProductoEntity> filas; }
    public static class ParteVentas { public List<VentaEntity> filas; }
    public static class ParteCierres { public List<CierreCajaEntity> filas; }
    public static class ParteBitacora { public List<BitacoraEntity> filas; }
    public static class ParteMermas { public List<MermaEntity> filas; }

    private static final String TAG = "Respaldo";

    private static boolean configurado = false;

    private final FirebaseFirestore fs;

    public RespaldoNube() {
        fs = instancia();
    }

    /**
     * Sin cache en disco: la app ya guarda todo en Room, y asi una lectura nunca
     * devuelve un respaldo viejo guardado en el celular, ni quedan escrituras
     * "pendientes" que se suban solas horas despues.
     */
    private static synchronized FirebaseFirestore instancia() {
        Log.i(TAG, "Obteniendo Firestore");
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        if (!configurado) {
            try {
                fs.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .build());
            } catch (IllegalStateException yaIniciado) {
                // Firestore ya se habia usado en este proceso: se queda como estaba.
            }
            configurado = true;
        }
        return fs;
    }

    private DocumentReference negocio(String uid) {
        return fs.collection("negocios").document(uid);
    }

    // ---------------------------------------------------------------- subir

    public InfoRespaldo subir(String uid, RespaldoCompleto r, String dispositivo) throws Exception {
        long ahora = System.currentTimeMillis();
        String id = String.valueOf(ahora);
        DocumentReference docRespaldo = negocio(uid).collection("respaldos").document(id);
        CollectionReference partes = docRespaldo.collection("partes");

        Log.i(TAG, "Paso 1: marcando respaldo " + id);
        // 1. Marca el respaldo como "en proceso" (sirve para limpiar si se queda a medias).
        Map<String, Object> marca = new HashMap<>();
        marca.put("fechaMs", ahora);
        marca.put("completo", false);
        esperar(docRespaldo.set(marca));

        // 2. Sube todas las partes en lotes.
        List<Map<String, Object>> docs = new ArrayList<>();
        agregarPartes(docs, T_CONFIG, Collections.singletonList(r.configuracion));
        agregarPartes(docs, T_USUARIOS, r.usuarios);
        agregarPartes(docs, T_PRODUCTOS, r.productos);
        agregarPartes(docs, T_VENTAS, r.ventas);
        agregarPartes(docs, T_CIERRES, r.cierres);
        agregarPartes(docs, T_BITACORA, r.bitacora);
        agregarPartes(docs, T_MERMAS, r.mermas);

        Log.i(TAG, "Paso 2: subiendo " + docs.size() + " partes");
        WriteBatch lote = fs.batch();
        int enLote = 0;
        for (Map<String, Object> d : docs) {
            lote.set(partes.document((String) d.remove("_id")), d);
            if (++enLote == OPS_POR_LOTE) {
                esperar(lote.commit());
                lote = fs.batch();
                enLote = 0;
            }
        }
        if (enLote > 0) {
            Log.i(TAG, "  enviando lote de " + enLote + " partes");
            esperar(lote.commit());
        }
        Log.i(TAG, "Paso 3: actualizando resumen");

        // 3. Ahora si: el resumen apunta al respaldo nuevo.
        InfoRespaldo info = new InfoRespaldo();
        info.id = id;
        info.fechaMs = ahora;
        info.dispositivo = dispositivo;
        info.nombreNegocio = r.configuracion.nombreNegocio;
        info.usuarios = r.usuarios.size();
        info.productos = r.productos.size();
        info.ventas = r.ventas.size();
        info.cierres = r.cierres.size();
        info.bitacora = r.bitacora.size();
        info.mermas = r.mermas.size();

        Map<String, Object> conteos = new HashMap<>();
        conteos.put(T_USUARIOS, info.usuarios);
        conteos.put(T_PRODUCTOS, info.productos);
        conteos.put(T_VENTAS, info.ventas);
        conteos.put(T_CIERRES, info.cierres);
        conteos.put(T_BITACORA, info.bitacora);
        conteos.put(T_MERMAS, info.mermas);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("respaldoActual", id);
        resumen.put("fechaMs", ahora);
        resumen.put("dispositivo", dispositivo);
        resumen.put("nombreNegocio", info.nombreNegocio);
        resumen.put("giro", r.configuracion.giro != null ? r.configuracion.giro.name() : null);
        resumen.put("versionModelo", VERSION_MODELO);
        resumen.put("conteos", conteos);

        WriteBatch fin = fs.batch();
        fin.update(docRespaldo, "completo", true);
        fin.set(negocio(uid), resumen);
        esperar(fin.commit());

        Log.i(TAG, "Paso 4: respaldo completo, limpiando viejos");
        // 4. Limpieza de respaldos viejos o incompletos (si falla, no pasa nada).
        try {
            borrarRespaldosExcepto(uid, id);
        } catch (Exception ignorada) {
            // Se vuelve a intentar en el siguiente respaldo.
        }
        return info;
    }

    private static void agregarPartes(List<Map<String, Object>> docs, String tabla, List<?> filas) {
        List<? extends List<?>> pedazos = Particion.partir(filas, FILAS_POR_PARTE);
        for (int i = 0; i < pedazos.size(); i++) {
            Map<String, Object> d = new HashMap<>();
            d.put("_id", Particion.idParte(tabla, i));
            d.put("tabla", tabla);
            d.put("indice", i);
            d.put("filas", pedazos.get(i));
            docs.add(d);
        }
    }

    private void borrarRespaldosExcepto(String uid, String idVigente) throws Exception {
        QuerySnapshot respaldos = esperar(negocio(uid).collection("respaldos").get(Source.SERVER));
        for (DocumentSnapshot viejo : respaldos.getDocuments()) {
            if (viejo.getId().equals(idVigente)) continue;
            QuerySnapshot partes = esperar(viejo.getReference().collection("partes").get(Source.SERVER));
            WriteBatch lote = fs.batch();
            int enLote = 0;
            for (DocumentSnapshot p : partes.getDocuments()) {
                lote.delete(p.getReference());
                if (++enLote == OPS_POR_LOTE) {
                    esperar(lote.commit());
                    lote = fs.batch();
                    enLote = 0;
                }
            }
            lote.delete(viejo.getReference());
            esperar(lote.commit());
        }
    }

    // ---------------------------------------------------------------- consultar

    /** Resumen del respaldo vigente, o null si esta cuenta nunca ha respaldado. */
    public InfoRespaldo consultar(String uid) throws Exception {
        DocumentSnapshot d = esperar(negocio(uid).get(Source.SERVER));
        if (!d.exists() || d.getString("respaldoActual") == null) return null;
        InfoRespaldo info = new InfoRespaldo();
        info.id = d.getString("respaldoActual");
        info.fechaMs = numero(d.get("fechaMs"));
        info.dispositivo = d.getString("dispositivo");
        info.nombreNegocio = d.getString("nombreNegocio");
        Object c = d.get("conteos");
        if (c instanceof Map) {
            Map<?, ?> conteos = (Map<?, ?>) c;
            info.usuarios = numero(conteos.get(T_USUARIOS));
            info.productos = numero(conteos.get(T_PRODUCTOS));
            info.ventas = numero(conteos.get(T_VENTAS));
            info.cierres = numero(conteos.get(T_CIERRES));
            info.bitacora = numero(conteos.get(T_BITACORA));
            info.mermas = numero(conteos.get(T_MERMAS));
        }
        return info;
    }

    private static long numero(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0;
    }

    // ---------------------------------------------------------------- bajar

    /** Descarga el respaldo vigente completo. null si no hay. */
    public RespaldoCompleto bajar(String uid) throws Exception {
        InfoRespaldo info = consultar(uid);
        if (info == null) return null;

        QuerySnapshot partes = esperar(negocio(uid).collection("respaldos").document(info.id)
                .collection("partes").get(Source.SERVER));

        // Ordena por id ("ventas_0000", "ventas_0001"...) para conservar el orden original.
        List<DocumentSnapshot> ordenadas = new ArrayList<>(partes.getDocuments());
        Collections.sort(ordenadas, (a, b) -> a.getId().compareTo(b.getId()));

        RespaldoCompleto r = new RespaldoCompleto();
        for (DocumentSnapshot p : ordenadas) {
            String tabla = p.getString("tabla");
            if (tabla == null) continue;
            switch (tabla) {
                case T_CONFIG: {
                    ParteConfiguracion x = p.toObject(ParteConfiguracion.class);
                    if (x != null && x.filas != null && !x.filas.isEmpty()) r.configuracion = x.filas.get(0);
                    break;
                }
                case T_USUARIOS: {
                    ParteUsuarios x = p.toObject(ParteUsuarios.class);
                    if (x != null && x.filas != null) r.usuarios.addAll(x.filas);
                    break;
                }
                case T_PRODUCTOS: {
                    ParteProductos x = p.toObject(ParteProductos.class);
                    if (x != null && x.filas != null) r.productos.addAll(x.filas);
                    break;
                }
                case T_VENTAS: {
                    ParteVentas x = p.toObject(ParteVentas.class);
                    if (x != null && x.filas != null) r.ventas.addAll(x.filas);
                    break;
                }
                case T_CIERRES: {
                    ParteCierres x = p.toObject(ParteCierres.class);
                    if (x != null && x.filas != null) r.cierres.addAll(x.filas);
                    break;
                }
                case T_BITACORA: {
                    ParteBitacora x = p.toObject(ParteBitacora.class);
                    if (x != null && x.filas != null) r.bitacora.addAll(x.filas);
                    break;
                }
                case T_MERMAS: {
                    ParteMermas x = p.toObject(ParteMermas.class);
                    if (x != null && x.filas != null) r.mermas.addAll(x.filas);
                    break;
                }
                default:
                    break;
            }
        }

        // Si falta algo (una parte que no se bajo), mejor no restaurar a medias.
        if (r.configuracion == null
                || r.usuarios.size() != info.usuarios
                || r.productos.size() != info.productos
                || r.ventas.size() != info.ventas
                || r.cierres.size() != info.cierres
                || r.bitacora.size() != info.bitacora
                || r.mermas.size() != info.mermas) {
            throw new RespaldoException(RespaldoException.Tipo.INCOMPLETO, null);
        }
        return r;
    }

    private static <T> T esperar(Task<T> tarea) throws Exception {
        return Tasks.await(tarea, TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }
}
