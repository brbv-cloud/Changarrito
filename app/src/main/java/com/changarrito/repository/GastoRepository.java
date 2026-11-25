package com.changarrito.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.GastoEntity;
import com.changarrito.database.entity.TipoGasto;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.reportes.CalculoGanancias;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Registrar y borrar gastos del negocio. Cada uno deja su linea en la bitacora
 * (quien, de que y cuanto), para que el dueno vea lo que se saco de la caja.
 *
 * El trabajador solo puede registrar gastos pagados con la caja, con la hora de ese
 * momento, y no puede registrar renta ni sueldos.
 */
public class GastoRepository {

    /** valorAnterior de la bitacora cuando el gasto salio de la caja. */
    public static final String MARCA_CAJA = "caja";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onListo();

        void onError(String mensaje);
    }

    private final Application app;
    private final AppDatabase db;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public GastoRepository(Application app) {
        this.app = app;
        this.db = AppDatabase.getInstance(app);
    }

    /**
     * @param fechaMs dia del gasto; 0 = ahora. Se ignora (es ahora) si se pago con la caja.
     */
    public void registrar(TipoGasto tipo, double monto, String nota, boolean conCaja, long fechaMs, Callback callback) {
        boolean esDueno = SesionTurno.esDueno(app);
        EXECUTOR.execute(() -> {
            String error;
            try {
                error = registrarSync(tipo, monto, nota, conCaja, fechaMs, esDueno, System.currentTimeMillis());
            } catch (Exception e) {
                error = "No se pudo registrar el gasto.";
            }
            String e = error;
            principal.post(() -> {
                if (e != null) callback.onError(e);
                else callback.onListo();
            });
        });
    }

    /** Sincrono (hilo de fondo). Devuelve el mensaje de error, o null si se guardo. */
    public String registrarSync(TipoGasto tipo, double monto, String nota, boolean conCaja, long fechaMs,
                                boolean esDueno, long ahora) {
        if (tipo == null) return "Elige de qué es el gasto.";
        if (!(monto > 0)) return "Escribe cuánto pagaste.";
        if (!esDueno && tipo.soloDueno) return "Ese gasto lo registra el dueño.";
        // El trabajador solo anota lo que saca del cajon, y en ese momento.
        boolean deCaja = conCaja || !esDueno;
        long cuando = deCaja || fechaMs <= 0 || fechaMs > ahora ? ahora : fechaMs;

        GastoEntity g = new GastoEntity();
        g.tipo = tipo;
        g.monto = Math.round(monto * 100) / 100.0;
        g.nota = nota != null && !nota.trim().isEmpty() ? nota.trim() : null;
        g.pagadoConCaja = deCaja;
        g.timestampMs = cuando;
        g.idUsuario = usuarioDelTurno();
        db.runInTransaction(() -> {
            db.gastoDao().insert(g);
            db.bitacoraDao().insert(bitacora(AccionBitacora.REGISTRAR_GASTO, g, ahora));
        });
        return null;
    }

    /** Borra un gasto mal capturado (solo el dueno llega a la lista de gastos). */
    public void borrar(int idGasto, Callback callback) {
        EXECUTOR.execute(() -> {
            String error = null;
            try {
                GastoEntity g = db.gastoDao().getByIdSync(idGasto);
                if (g == null) {
                    error = "Ese gasto ya no existe.";
                } else {
                    long ahora = System.currentTimeMillis();
                    db.runInTransaction(() -> {
                        db.gastoDao().borrar(idGasto);
                        db.bitacoraDao().insert(bitacora(AccionBitacora.BORRAR_GASTO, g, ahora));
                    });
                }
            } catch (Exception e) {
                error = "No se pudo borrar el gasto.";
            }
            String e = error;
            principal.post(() -> {
                if (e != null) callback.onError(e);
                else callback.onListo();
            });
        });
    }

    private BitacoraEntity bitacora(AccionBitacora accion, GastoEntity g, long ahora) {
        BitacoraEntity b = new BitacoraEntity();
        b.timestampMs = ahora;
        b.idUsuario = usuarioDelTurno();
        b.idProducto = null;
        b.nombreProducto = g.nota; // la nota del gasto ("recibo de octubre"), puede ir vacia
        b.accion = accion;
        b.campo = g.tipo != null ? g.tipo.name() : null;
        b.valorAnterior = g.pagadoConCaja ? MARCA_CAJA : null;
        b.valorNuevo = CalculoGanancias.dinero(g.monto);
        return b;
    }

    private Integer usuarioDelTurno() {
        Integer id = SesionTurno.getIdUsuario(app);
        if (id == null) return null;
        return db.usuarioDao().getUsuarioByIdSync(id) != null ? id : null;
    }
}
