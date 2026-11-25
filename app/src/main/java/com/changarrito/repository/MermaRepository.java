package com.changarrito.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.MermaEntity;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.negocio.SesionTurno;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Registra mermas: en una sola transaccion guarda la merma, descuenta la existencia
 * del producto y deja la entrada en la bitacora (quien, que y por que).
 * Lo pueden usar el dueno y los trabajadores.
 */
public class MermaRepository {

    private static final double EPSILON = 0.0001;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        /** @param quedan existencia que quedo despues de la merma */
        void onRegistrada(double quedan);

        void onError(String mensaje);
    }

    private final Application app;
    private final AppDatabase db;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public MermaRepository(Application app) {
        this.app = app;
        this.db = AppDatabase.getInstance(app);
    }

    public void registrar(int idProducto, double cantidad, MotivoMerma motivo, String nota, Callback callback) {
        EXECUTOR.execute(() -> {
            String[] error = new String[1];
            double[] quedan = new double[1];
            try {
                registrarSync(idProducto, cantidad, motivo, nota, System.currentTimeMillis(), error, quedan);
            } catch (Exception e) {
                error[0] = "No se pudo registrar la merma.";
            }
            principal.post(() -> {
                if (error[0] != null) callback.onError(error[0]);
                else callback.onRegistrada(quedan[0]);
            });
        });
    }

    /** Sincrono (hilo de fondo). error[0] queda con el mensaje si no se pudo; quedan[0] con la existencia final. */
    public void registrarSync(int idProducto, double cantidad, MotivoMerma motivo, String nota, long ahora,
                              String[] error, double[] quedan) {
        db.runInTransaction(() -> {
            ProductoEntity p = db.productoDao().getProductoByIdSync(idProducto);
            if (p == null || !p.activo) {
                error[0] = "El producto ya no está en el inventario.";
                return;
            }
            if (cantidad <= 0) {
                error[0] = "La cantidad debe ser mayor a 0.";
                return;
            }
            if (p.unidadMedida == UnidadMedida.PZA && Math.abs(cantidad - Math.rint(cantidad)) > EPSILON) {
                error[0] = p.nombre + " va por pieza: la cantidad no lleva decimales.";
                return;
            }
            if (cantidad > p.cantidadDisponible + EPSILON) {
                error[0] = "Solo hay " + cantidadTexto(p.cantidadDisponible, p.unidadMedida) + " de " + p.nombre + ".";
                return;
            }
            double antes = p.cantidadDisponible;
            double despues = antes - cantidad;
            if (Math.abs(despues) < EPSILON) despues = 0;
            p.cantidadDisponible = despues;
            p.timestampActualizacionMs = ahora;
            db.productoDao().update(p);

            Integer quien = usuarioDelTurno();
            MermaEntity m = new MermaEntity();
            m.idProducto = p.id;
            m.nombreProducto = p.nombre;
            m.cantidad = cantidad;
            m.unidadMedida = p.unidadMedida;
            m.costoUnitario = p.costoCompra;
            m.motivo = motivo != null ? motivo : MotivoMerma.OTRO;
            m.nota = nota != null && !nota.trim().isEmpty() ? nota.trim() : null;
            m.timestampMs = ahora;
            m.idUsuario = quien;
            db.mermaDao().insert(m);

            BitacoraEntity b = new BitacoraEntity();
            b.timestampMs = ahora;
            b.idUsuario = quien;
            b.idProducto = p.id;
            b.nombreProducto = p.nombre;
            b.accion = AccionBitacora.REGISTRAR_MERMA;
            b.campo = m.motivo.name();
            b.valorAnterior = cantidadTexto(antes, p.unidadMedida);
            b.valorNuevo = cantidadTexto(despues, p.unidadMedida);
            db.bitacoraDao().insert(b);

            quedan[0] = despues;
        });
    }

    private Integer usuarioDelTurno() {
        Integer id = SesionTurno.getIdUsuario(app);
        if (id == null) return null;
        return db.usuarioDao().getUsuarioByIdSync(id) != null ? id : null;
    }

    /** "12 PZA", "0.750 KG". */
    public static String cantidadTexto(double valor, UnidadMedida unidad) {
        String u = unidad != null ? " " + unidad.name() : "";
        if (unidad == UnidadMedida.PZA || Math.abs(valor - Math.rint(valor)) < 0.0005) {
            return String.format(Locale.US, "%.0f%s", valor, u);
        }
        return String.format(Locale.US, "%.3f%s", valor, u);
    }
}
