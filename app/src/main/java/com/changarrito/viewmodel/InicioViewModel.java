package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.reportes.RangoReporte;

/** Datos de la pantalla de Inicio: productos, ventas de hoy y alertas activas. */
public class InicioViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final LiveData<Integer> productos;
    private final LiveData<Integer> alertas;
    private final LiveData<ConfiguracionNegocioEntity> configuracion;
    private final MediatorLiveData<ResumenGanancia> hoy = new MediatorLiveData<>();
    private LiveData<ResumenGanancia> fuenteHoy;
    private long inicioHoyMs = -1;
    private final LiveData<Integer> pendientes;
    private final MediatorLiveData<Double> promedioDiario = new MediatorLiveData<>();
    private LiveData<Double> fuentePromedio;

    public InicioViewModel(@NonNull Application app) {
        super(app);
        db = AppDatabase.getInstance(app);
        productos = db.productoDao().contarActivos();
        alertas = db.alertaDao().contarPorEstado(EstadoAlerta.ACTIVA);
        configuracion = db.configuracionNegocioDao().getConfiguracion();
        pendientes = db.productoDao().contarPendientes();
        refrescarHoy();
    }

    /** "Hoy" cambia a medianoche: se recalcula cada vez que se vuelve a Inicio. */
    public void refrescarHoy() {
        long[] r = RangoReporte.HOY.calcular(System.currentTimeMillis());
        if (r[0] == inicioHoyMs) return;
        inicioHoyMs = r[0];
        if (fuenteHoy != null) hoy.removeSource(fuenteHoy);
        fuenteHoy = db.ventaDao().getResumenGanancia(r[0], r[1]);
        hoy.addSource(fuenteHoy, hoy::setValue);
        // "Dia normal": promedio de los ultimos 30 dias (sin contar hoy).
        if (fuentePromedio != null) promedioDiario.removeSource(fuentePromedio);
        fuentePromedio = db.ventaDao().promedioVentasPorDia(r[0] - 30L * 24 * 60 * 60 * 1000, r[0]);
        promedioDiario.addSource(fuentePromedio, promedioDiario::setValue);
    }

    public LiveData<Integer> getProductos() {
        return productos;
    }

    public LiveData<Integer> getAlertas() {
        return alertas;
    }

    public LiveData<ResumenGanancia> getHoy() {
        return hoy;
    }

    public LiveData<Integer> getPendientes() {
        return pendientes;
    }

    public LiveData<Double> getPromedioDiario() {
        return promedioDiario;
    }

    public LiveData<ConfiguracionNegocioEntity> getConfiguracion() {
        return configuracion;
    }
}
