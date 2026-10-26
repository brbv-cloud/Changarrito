package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaDia;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.database.dao.VentasPorUsuario;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.DiaReporte;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.reportes.SerieDiaria;
import com.changarrito.repository.GananciasRepository;

import java.util.List;
import java.util.function.Function;

/**
 * Estado de la pantalla de Reportes. Al cambiar el periodo (Hoy / 7 / 30 dias)
 * todas las consultas se rehacen solas. "Productos sin moverse" usa su propio
 * filtro (30 o 60 dias), independiente del periodo.
 */
public class GananciasViewModel extends AndroidViewModel {

    private static final long DIA_MS = 24L * 60 * 60 * 1000;

    private final MutableLiveData<RangoReporte> rango = new MutableLiveData<>(RangoReporte.HOY);
    private final MutableLiveData<Integer> diasHueso = new MutableLiveData<>(30);

    private final LiveData<ResumenGanancia> resumen;
    private final LiveData<List<GananciaProducto>> porProducto;
    private final LiveData<List<VentasPorHora>> porHora;
    private final LiveData<List<VentasPorCategoria>> porCategoria;
    private final LiveData<List<MermaPorMotivo>> mermasPorMotivo;
    private final LiveData<List<MermaConUsuario>> mermas;
    private final LiveData<List<ProductoHueso>> huesos;
    private final MediatorLiveData<List<DiaReporte>> porDia = new MediatorLiveData<>();
    private final MediatorLiveData<List<CalculoGanancias.FilaPersona>> porPersona = new MediatorLiveData<>();

    private List<VentasPorUsuario> ultimasVentas;
    private List<CierresPorUsuario> ultimosCierres;

    public GananciasViewModel(@NonNull Application application) {
        super(application);
        GananciasRepository repo = new GananciasRepository(application);

        resumen = segun(rango, r -> repo.getResumen(rangoMs(r)));
        porProducto = segun(rango, r -> repo.getPorProducto(rangoMs(r)));
        porHora = segun(rango, r -> repo.getPorHora(rangoMs(r)));
        porCategoria = segun(rango, r -> repo.getPorCategoria(rangoMs(r)));
        mermasPorMotivo = segun(rango, r -> repo.getMermasPorMotivo(rangoMs(r)));
        mermas = segun(rango, r -> repo.getMermas(rangoMs(r)));
        huesos = segun(diasHueso, d -> repo.getHuesos(System.currentTimeMillis() - d * DIA_MS));

        // Dia por dia: la consulta solo trae dias con ventas; aqui se rellenan los demas.
        LiveData<List<GananciaDia>> diasCrudos = segun(rango, r -> repo.getPorDia(rangoMs(r)));
        porDia.addSource(diasCrudos, lista -> {
            long[] r = rangoMs(getRangoActual());
            porDia.setValue(SerieDiaria.completar(lista, r[0], r[1]));
        });

        LiveData<List<VentasPorUsuario>> ventas = segun(rango, r -> repo.getVentasPorUsuario(rangoMs(r)));
        LiveData<List<CierresPorUsuario>> cierres = segun(rango, r -> repo.getCierresPorUsuario(rangoMs(r)));
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
     * Equivalente a Transformations.switchMap: cuando cambia la fuente deja de
     * escuchar la consulta anterior y escucha la nueva. Se hace a mano porque en
     * lifecycle 2.6.x las sobrecargas de Transformations salen ambiguas en Java.
     */
    private static <X, T> LiveData<T> segun(LiveData<X> fuente, Function<X, LiveData<T>> consulta) {
        Conmutador<T> salida = new Conmutador<>();
        salida.addSource(fuente, x -> salida.cambiarA(consulta.apply(x)));
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

    public RangoReporte getRangoActual() {
        return rango.getValue() != null ? rango.getValue() : RangoReporte.HOY;
    }

    public void setDiasHueso(int dias) {
        Integer actual = diasHueso.getValue();
        if (actual == null || actual != dias) diasHueso.setValue(dias);
    }

    public LiveData<ResumenGanancia> getResumen() {
        return resumen;
    }

    public LiveData<List<GananciaProducto>> getPorProducto() {
        return porProducto;
    }

    public LiveData<List<DiaReporte>> getPorDia() {
        return porDia;
    }

    public LiveData<List<VentasPorHora>> getPorHora() {
        return porHora;
    }

    public LiveData<List<VentasPorCategoria>> getPorCategoria() {
        return porCategoria;
    }

    public LiveData<List<MermaPorMotivo>> getMermasPorMotivo() {
        return mermasPorMotivo;
    }

    public LiveData<List<MermaConUsuario>> getMermas() {
        return mermas;
    }

    public LiveData<List<ProductoHueso>> getHuesos() {
        return huesos;
    }

    public LiveData<List<CalculoGanancias.FilaPersona>> getPorPersona() {
        return porPersona;
    }
}
