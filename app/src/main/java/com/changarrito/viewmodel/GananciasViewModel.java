package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorUsuario;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.repository.GananciasRepository;

import java.util.List;
import java.util.function.Function;

/**
 * Estado del reporte de Ganancias. Al cambiar el rango (Hoy / 7 / 30 dias)
 * todas las consultas se rehacen solas (ver segunRango).
 */
public class GananciasViewModel extends AndroidViewModel {

    public static final int MAX_MENOS_RENTABLES = 5;

    private final MutableLiveData<RangoReporte> rango = new MutableLiveData<>(RangoReporte.HOY);

    private final LiveData<ResumenGanancia> resumen;
    private final LiveData<List<GananciaProducto>> porProducto;
    private final LiveData<List<GananciaProducto>> menosRentables;
    private final MediatorLiveData<List<CalculoGanancias.FilaPersona>> porPersona = new MediatorLiveData<>();

    private List<VentasPorUsuario> ultimasVentas;
    private List<CierresPorUsuario> ultimosCierres;

    public GananciasViewModel(@NonNull Application application) {
        super(application);
        GananciasRepository repo = new GananciasRepository(application);

        resumen = segunRango(r -> repo.getResumen(rangoMs(r)));
        porProducto = segunRango(r -> repo.getPorProducto(rangoMs(r)));

        MediatorLiveData<List<GananciaProducto>> bajos = new MediatorLiveData<>();
        bajos.addSource(porProducto,
                lista -> bajos.setValue(CalculoGanancias.menosRentables(lista, MAX_MENOS_RENTABLES)));
        menosRentables = bajos;

        LiveData<List<VentasPorUsuario>> ventas = segunRango(r -> repo.getVentasPorUsuario(rangoMs(r)));
        LiveData<List<CierresPorUsuario>> cierres = segunRango(r -> repo.getCierresPorUsuario(rangoMs(r)));
        porPersona.addSource(ventas, v -> {
            ultimasVentas = v;
            porPersona.setValue(CalculoGanancias.combinarPersonas(ultimasVentas, ultimosCierres));
        });
        porPersona.addSource(cierres, c -> {
            ultimosCierres = c;
            porPersona.setValue(CalculoGanancias.combinarPersonas(ultimasVentas, ultimosCierres));
        });
    }

    /**
     * Equivalente a Transformations.switchMap: cada vez que cambia el rango deja
     * de escuchar la consulta anterior y escucha la nueva. Se hace a mano porque
     * en lifecycle 2.6.x las sobrecargas de Transformations salen ambiguas en Java.
     */
    private <T> LiveData<T> segunRango(Function<RangoReporte, LiveData<T>> consulta) {
        Conmutador<T> salida = new Conmutador<>();
        salida.addSource(rango, r -> salida.cambiarA(consulta.apply(r)));
        return salida;
    }

    private static class Conmutador<T> extends MediatorLiveData<T> {
        private LiveData<T> actual;

        void cambiarA(LiveData<T> nueva) {
            if (actual == nueva) return;
            if (actual != null) removeSource(actual);
            actual = nueva;
            if (nueva != null) addSource(nueva, this::setValue);
        }
    }

    private static long[] rangoMs(RangoReporte r) {
        return r.calcular(System.currentTimeMillis());
    }

    public void setRango(RangoReporte nuevo) {
        if (nuevo != rango.getValue()) rango.setValue(nuevo);
    }

    public LiveData<ResumenGanancia> getResumen() {
        return resumen;
    }

    public LiveData<List<GananciaProducto>> getPorProducto() {
        return porProducto;
    }

    public LiveData<List<GananciaProducto>> getMenosRentables() {
        return menosRentables;
    }

    public LiveData<List<CalculoGanancias.FilaPersona>> getPorPersona() {
        return porPersona;
    }
}
