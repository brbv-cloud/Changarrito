package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierreCajaDAO;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.utils.FechaUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Arqueo / cierre de caja: compara efectivoSistema (calculado de las Ventas desde
 * el ultimo cierre, ver calcularEfectivoPendienteSync) contra lo que el comerciante
 * cuenta a mano. Es un historico de auditoria, no se permite editar un cierre ya
 * hecho: si el usuario cierra otra vez el mismo dia, el "dia" se restablece y el
 * siguiente cierre solo ve lo vendido despues del anterior.
 */
public class CierreCajaRepository {

    private final CierreCajaDAO cierreCajaDAO;
    private final VentaDAO ventaDAO;
    private final ExecutorService executorService;

    public CierreCajaRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        cierreCajaDAO = db.cierreCajaDao();
        ventaDAO = db.ventaDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<CierreCajaEntity> getCierreDeHoy() {
        return cierreCajaDAO.getCierreByFecha(FechaUtil.inicioDelDia(System.currentTimeMillis()));
    }

    public LiveData<List<CierreCajaEntity>> getHistorial() {
        return cierreCajaDAO.getHistorialCierres();
    }

    public interface RegistrarCierreCallback {
        void onResultado(CierreCajaEntity cierre);
    }

    public interface EfectivoPendienteCallback {
        void onResultado(double efectivo);
    }

    /**
     * Efectivo que el sistema dice que se ha vendido desde el ultimo cierre hecho
     * (de cualquier dia). Si nunca se ha hecho un cierre, cuenta desde el inicio
     * del dia actual. Esto es lo que hace que "se restablezca el dia" cada vez que
     * se cierra: un segundo cierre en la misma jornada solo ve lo vendido despues
     * del primero, no repite lo ya reconciliado.
     */
    private double calcularEfectivoPendienteSync() {
        CierreCajaEntity ultimoCierre = cierreCajaDAO.getUltimoCierreSync();
        long desdeMs = ultimoCierre != null
                ? ultimoCierre.timestampCreacionMs
                : FechaUtil.inicioDelDia(System.currentTimeMillis());
        return ventaDAO.getTotalVentasDesdeSync(desdeMs);
    }

    /** Para mostrar en pantalla, antes de cerrar, cuanto efectivo esta pendiente de reconciliar. */
    public void obtenerEfectivoPendiente(EfectivoPendienteCallback callback) {
        executorService.execute(() -> callback.onResultado(calcularEfectivoPendienteSync()));
    }

    /**
     * Calcula efectivoSistema desde el ultimo cierre (no el dia calendario completo,
     * ver calcularEfectivoPendienteSync), calcula la diferencia contra lo que el
     * usuario conto fisicamente, y guarda el registro. Corre en un hilo de fondo
     * propio; el callback llega en ese mismo hilo (el llamador decide si necesita
     * saltar al hilo principal para tocar UI).
     */
    public void registrarCierre(double efectivoFisicoContado, String notas, RegistrarCierreCallback callback) {
        executorService.execute(() -> {
            long ahora = System.currentTimeMillis();

            double efectivoSistema = calcularEfectivoPendienteSync();

            CierreCajaEntity cierre = new CierreCajaEntity();
            cierre.fechaMs = FechaUtil.inicioDelDia(ahora);
            cierre.efectivoSistema = efectivoSistema;
            cierre.efectivoFisicoContado = efectivoFisicoContado;
            cierre.diferencia = efectivoFisicoContado - efectivoSistema;
            cierre.notas = notas;
            cierre.timestampCreacionMs = ahora;

            cierreCajaDAO.insert(cierre);
            callback.onResultado(cierre);
        });
    }
}
