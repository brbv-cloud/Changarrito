package com.changarrito.negocio;

import android.app.Application;

import androidx.core.app.NotificationManagerCompat;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.RespaldoDAO;
import com.changarrito.database.entity.MermaEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.respaldo.RespaldoCompleto;
import com.changarrito.respaldo.RespaldoLocal;
import com.changarrito.respaldo.RespaldoRepository;

/**
 * Cuando entra a este celular una cuenta distinta a la del negocio que ya tiene.
 *
 * Un celular guarda un solo negocio. Para que la otra cuenta pueda usarlo hay que
 * borrar el negocio de aqui; antes se muestra que tiene y cuando se respaldo por
 * ultima vez, y se pide la clave del dueno de ese negocio (asi nadie borra un negocio
 * ajeno solo por iniciar sesion con su cuenta). El respaldo en la nube no se toca:
 * el dueno lo recupera entrando con su cuenta en cualquier celular.
 *
 * Los metodos *Sync van en un hilo de fondo.
 */
public final class CambioDeNegocio {

    /** Lo que hay en el celular, para explicarlo antes de borrar. */
    public static final class Resumen {
        public String nombreNegocio;
        public int productos;
        public int ventas;
        /** Dueno del negocio guardado; null si no hay perfiles. */
        public UsuarioEntity dueno;
        /** Ultimo cambio local (producto, venta o merma), para compararlo con el ultimo respaldo. */
        public long ultimoCambioMs;
        public long ultimoRespaldoMs;

        public boolean duenoConClave() {
            return dueno != null && dueno.claveHash != null;
        }

        /** Nunca respaldado, o hubo cambios despues del ultimo respaldo. */
        public boolean hayCambiosSinRespaldar() {
            return ultimoRespaldoMs <= 0 || ultimoCambioMs > ultimoRespaldoMs;
        }
    }

    private CambioDeNegocio() {
    }

    public static Resumen leerSync(Application app) {
        AppDatabase db = AppDatabase.getInstance(app);
        RespaldoCompleto foto = RespaldoLocal.leer(db);
        Resumen r = new Resumen();
        r.nombreNegocio = foto.configuracion != null ? foto.configuracion.nombreNegocio : null;
        for (ProductoEntity p : foto.productos) {
            if (p.activo) r.productos++;
            r.ultimoCambioMs = Math.max(r.ultimoCambioMs, p.timestampActualizacionMs);
        }
        r.ventas = foto.ventas.size();
        for (VentaEntity v : foto.ventas) r.ultimoCambioMs = Math.max(r.ultimoCambioMs, v.timestampVentaMs);
        if (foto.mermas != null) {
            for (MermaEntity m : foto.mermas) r.ultimoCambioMs = Math.max(r.ultimoCambioMs, m.timestampMs);
        }
        for (com.changarrito.database.entity.GastoEntity g : foto.gastos) {
            r.ultimoCambioMs = Math.max(r.ultimoCambioMs, g.timestampMs);
        }
        r.dueno = db.usuarioDao().getDuenoSync();
        r.ultimoRespaldoMs = new RespaldoRepository(app).getUltimoRespaldoMs();
        return r;
    }

    /**
     * Borra el negocio de este celular (todas las tablas, en una sola transaccion), cierra
     * el turno y olvida el ultimo respaldo y las notificaciones. Queda como recien instalada.
     */
    public static void borrarSync(Application app) {
        AppDatabase db = AppDatabase.getInstance(app);
        RespaldoDAO dao = db.respaldoDao();
        db.runInTransaction(() -> {
            dao.borrarAlertas();
            dao.borrarMermas();
            dao.borrarGastos();
            dao.borrarBitacora();
            dao.borrarVentas();
            dao.borrarCierres();
            dao.borrarProductos();
            dao.borrarUsuarios();
            dao.borrarConfiguracion();
        });
        SesionTurno.cerrar(app);
        RespaldoRepository.olvidarUltimo(app);
        NotificationManagerCompat.from(app).cancelAll();
    }
}
