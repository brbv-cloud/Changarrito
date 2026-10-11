package com.changarrito.negocio;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ConfiguracionNegocioDAO;
import com.changarrito.database.dao.UsuarioDAO;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.utils.ClaveUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuración del negocio, perfiles (dueño y trabajadores) y turnos.
 * Todo corre en un hilo de fondo y los callbacks llegan en el hilo principal.
 */
public class NegocioRepository {

    /** A dónde debe ir el usuario después de iniciar sesión. */
    public enum Destino { ELEGIR_GIRO, CATALOGO, ELEGIR_TURNO, APP, OTRA_CUENTA }

    public enum ResultadoClave { CORRECTA, INCORRECTA, BLOQUEADO }

    public interface Callback<T> {
        void onResultado(T resultado);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Application app;
    private final AppDatabase db;
    private final UsuarioDAO usuarioDao;
    private final ConfiguracionNegocioDAO configDao;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public NegocioRepository(Application app) {
        this.app = app;
        this.db = AppDatabase.getInstance(app);
        this.usuarioDao = db.usuarioDao();
        this.configDao = db.configuracionNegocioDao();
    }

    private <T> void enFondo(java.util.concurrent.Callable<T> trabajo, Callback<T> callback) {
        EXECUTOR.execute(() -> {
            T r;
            try {
                r = trabajo.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            T resultado = r;
            if (callback != null) principal.post(() -> callback.onResultado(resultado));
        });
    }

    public LiveData<ConfiguracionNegocioEntity> getConfiguracion() {
        return configDao.getConfiguracion();
    }

    public LiveData<List<UsuarioEntity>> getUsuariosActivos() {
        return usuarioDao.getUsuariosActivos();
    }

    /**
     * Decide la pantalla de entrada. Si el negocio tiene un solo perfil (el dueño), le
     * abre el turno solo: no tiene caso preguntar "¿quién atiende?".
     */
    public void resolverInicio(String uid, String nombreCuenta, Callback<Destino> callback) {
        enFondo(() -> {
            ConfiguracionNegocioEntity config = configDao.getConfiguracionSync();
            if (config == null) return Destino.ELEGIR_GIRO;
            // Un celular = un negocio. Si entra otra cuenta, no le mostramos datos ajenos.
            if (config.uidFirebase != null && !config.uidFirebase.equals(uid)) return Destino.OTRA_CUENTA;

            UsuarioEntity dueno = asegurarDueno(nombreCuenta);
            if (!config.catalogoImportado && config.giro != null && config.giro.archivoCatalogo != null) {
                return Destino.CATALOGO;
            }

            if (usuarioDao.contarActivosSync() <= 1) {
                SesionTurno.abrir(app, dueno.id, dueno.nombre, Rol.DUENO);
                return Destino.APP;
            }
            Integer idTurno = SesionTurno.getIdUsuario(app);
            if (idTurno != null) {
                UsuarioEntity u = usuarioDao.getUsuarioByIdSync(idTurno);
                if (u != null && u.activo) return Destino.APP;
            }
            SesionTurno.cerrar(app);
            return Destino.ELEGIR_TURNO;
        }, callback);
    }

    /** Guarda el giro elegido y crea el perfil del dueño. Devuelve true si el giro tiene catálogo. */
    public void guardarGiro(GiroNegocio giro, String nombreNegocio, String uid, String nombreDueno,
                            double margenPorDefecto, Callback<Boolean> callback) {
        enFondo(() -> {
            ConfiguracionNegocioEntity c = ConfiguracionNegocioEntity.paraGiro(giro, uid, margenPorDefecto);
            c.nombreNegocio = nombreNegocio;
            // Sin catálogo no hay nada que decidir: se marca como resuelto.
            c.catalogoImportado = giro.archivoCatalogo == null;
            configDao.guardar(c);
            asegurarDueno(nombreDueno);
            return giro.archivoCatalogo != null;
        }, callback);
    }

    /**
     * Inserta los productos elegidos (puede ser una lista vacía = "Empezar vacío") y
     * marca el catálogo como resuelto para no volver a ofrecerlo.
     */
    public void importarCatalogo(List<ProductoEntity> productos, Callback<Integer> callback) {
        enFondo(() -> {
            db.runInTransaction(() -> {
                if (!productos.isEmpty()) db.productoDao().insertAll(productos);
                ConfiguracionNegocioEntity c = configDao.getConfiguracionSync();
                if (c != null) {
                    c.catalogoImportado = true;
                    c.timestampActualizacionMs = System.currentTimeMillis();
                    configDao.guardar(c);
                }
            });
            return productos.size();
        }, callback);
    }

    private UsuarioEntity asegurarDueno(String nombre) {
        UsuarioEntity dueno = usuarioDao.getDuenoSync();
        if (dueno != null) return dueno;
        dueno = new UsuarioEntity();
        dueno.nombre = (nombre == null || nombre.trim().isEmpty()) ? "Dueño" : nombre.trim();
        dueno.rol = Rol.DUENO;
        dueno.timestampCreacionMs = System.currentTimeMillis();
        dueno.id = (int) usuarioDao.insert(dueno);
        return dueno;
    }

    public void getDueno(Callback<UsuarioEntity> callback) {
        enFondo(usuarioDao::getDuenoSync, callback);
    }

    public void crearTrabajador(String nombre, String clave, Callback<Integer> callback) {
        enFondo(() -> {
            UsuarioEntity u = new UsuarioEntity();
            u.nombre = nombre.trim();
            u.rol = Rol.TRABAJADOR;
            u.claveSal = ClaveUtil.generarSal();
            u.claveHash = ClaveUtil.hashear(clave, u.claveSal);
            u.timestampCreacionMs = System.currentTimeMillis();
            return (int) usuarioDao.insert(u);
        }, callback);
    }

    public void establecerClave(int idUsuario, String clave, Callback<Boolean> callback) {
        enFondo(() -> {
            UsuarioEntity u = usuarioDao.getUsuarioByIdSync(idUsuario);
            if (u == null) return false;
            u.claveSal = ClaveUtil.generarSal();
            u.claveHash = ClaveUtil.hashear(clave, u.claveSal);
            u.intentosFallidos = 0;
            u.bloqueadoHastaMs = 0;
            usuarioDao.update(u);
            return true;
        }, callback);
    }

    /** Baja suave: el perfil deja de aparecer, pero sus ventas conservan quién las hizo. */
    public void darDeBaja(int idUsuario, Callback<Boolean> callback) {
        enFondo(() -> {
            UsuarioEntity u = usuarioDao.getUsuarioByIdSync(idUsuario);
            if (u == null || u.rol == Rol.DUENO) return false;
            u.activo = false;
            usuarioDao.update(u);
            return true;
        }, callback);
    }

    /** Verifica la clave de un perfil, contando intentos fallidos y bloqueando si hace falta. */
    public void verificarClave(int idUsuario, String clave, Callback<Object[]> callback) {
        enFondo(() -> {
            UsuarioEntity u = usuarioDao.getUsuarioByIdSync(idUsuario);
            long ahora = System.currentTimeMillis();
            if (u == null) return new Object[]{ResultadoClave.INCORRECTA, 0L};
            long restante = ControlIntentos.msRestantes(u.bloqueadoHastaMs, ahora);
            if (restante > 0) return new Object[]{ResultadoClave.BLOQUEADO, restante};
            // Un perfil sin clave (dueño que aún no crea la suya) entra directo.
            if (u.claveHash == null || ClaveUtil.verificar(clave, u.claveSal, u.claveHash)) {
                u.intentosFallidos = 0;
                u.bloqueadoHastaMs = 0;
                usuarioDao.update(u);
                SesionTurno.abrir(app, u.id, u.nombre, u.rol);
                return new Object[]{ResultadoClave.CORRECTA, 0L};
            }
            long[] estado = ControlIntentos.registrarFallo(u.intentosFallidos, ahora);
            u.intentosFallidos = (int) estado[0];
            u.bloqueadoHastaMs = estado[1];
            usuarioDao.update(u);
            if (u.bloqueadoHastaMs > 0) return new Object[]{ResultadoClave.BLOQUEADO, ControlIntentos.BLOQUEO_MS};
            return new Object[]{ResultadoClave.INCORRECTA, (long) ControlIntentos.intentosRestantes(u.intentosFallidos)};
        }, callback);
    }
}
