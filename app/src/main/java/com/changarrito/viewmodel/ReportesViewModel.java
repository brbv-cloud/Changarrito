package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.changarrito.database.dao.IngresoDia;
import com.changarrito.database.dao.ProductoConTotal;
import com.changarrito.repository.ReportesRepository;

import java.util.List;

public class ReportesViewModel extends AndroidViewModel {

    private static final int DIAS_GRAFICA = 7;
    private static final int TOP_PRODUCTOS = 5;

    private final ReportesRepository repository;

    private final LiveData<Double> ingresosHoy;
    private final LiveData<Integer> transaccionesHoy;
    private final LiveData<ProductoConTotal> productoTopHoy;
    private final LiveData<List<IngresoDia>> ingresosUltimos7Dias;
    private final LiveData<List<ProductoConTotal>> productosMasVendidos;

    public ReportesViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportesRepository(application);

        ingresosHoy = repository.getIngresosHoy();
        transaccionesHoy = repository.getTransaccionesHoy();
        productoTopHoy = repository.getProductoTopHoy();
        ingresosUltimos7Dias = repository.getIngresosUltimosDias(DIAS_GRAFICA);
        productosMasVendidos = repository.getProductosMasVendidos(TOP_PRODUCTOS);
    }

    public LiveData<Double> getIngresosHoy() {
        return ingresosHoy;
    }

    public LiveData<Integer> getTransaccionesHoy() {
        return transaccionesHoy;
    }

    public LiveData<ProductoConTotal> getProductoTopHoy() {
        return productoTopHoy;
    }

    public LiveData<List<IngresoDia>> getIngresosUltimos7Dias() {
        return ingresosUltimos7Dias;
    }

    public LiveData<List<ProductoConTotal>> getProductosMasVendidos() {
        return productosMasVendidos;
    }
}
