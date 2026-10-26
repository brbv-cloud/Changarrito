package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.repository.CierreCajaRepository;
import com.changarrito.workers.RespaldoWorker;

import java.util.List;

public class CierreCajaViewModel extends AndroidViewModel {

    private final CierreCajaRepository repository;
    private final LiveData<CierreCajaEntity> cierreDeHoy;
    private final LiveData<List<CierreCajaEntity>> historial;
    private final MutableLiveData<CierreCajaEntity> ultimoResultado = new MutableLiveData<>();
    private final MutableLiveData<Double> efectivoPendienteDeCierre = new MutableLiveData<>(0.0);

    public CierreCajaViewModel(@NonNull Application application) {
        super(application);
        repository = new CierreCajaRepository(application);
        cierreDeHoy = repository.getCierreDeHoy();
        historial = repository.getHistorial();
    }

    public LiveData<CierreCajaEntity> getCierreDeHoy() {
        return cierreDeHoy;
    }

    public LiveData<List<CierreCajaEntity>> getHistorial() {
        return historial;
    }

    /** Se emite justo despues de registrar un cierre, para que la UI muestre el resultado (sobrante/faltante/exacto). */
    public LiveData<CierreCajaEntity> getUltimoResultado() {
        return ultimoResultado;
    }

    /** Efectivo vendido desde el ultimo cierre (o desde el inicio del dia si no hay ninguno todavia). */
    public LiveData<Double> getEfectivoPendienteDeCierre() {
        return efectivoPendienteDeCierre;
    }

    /** Refresca efectivoPendienteDeCierre; llamar al mostrar la pestaña de Caja. */
    public void actualizarEfectivoPendiente() {
        repository.obtenerEfectivoPendiente(efectivoPendienteDeCierre::postValue);
    }

    public void hacerCierre(double efectivoContado, String notas) {
        repository.registrarCierre(efectivoContado, notas, cierre -> {
            ultimoResultado.postValue(cierre);
            actualizarEfectivoPendiente();
            // El cierre es el final natural del dia: se respalda en cuanto haya internet.
            RespaldoWorker.respaldarPronto(getApplication());
        });
    }
}
