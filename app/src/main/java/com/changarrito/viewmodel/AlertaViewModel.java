package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.repository.AlertaRepository;

import java.util.List;

public class AlertaViewModel extends AndroidViewModel {

    private final AlertaRepository repository;
    private final LiveData<List<AlertaConProducto>> alertasActivas;
    private final LiveData<Integer> conteoAlertasActivas;

    public AlertaViewModel(@NonNull Application application) {
        super(application);
        repository = new AlertaRepository(application);
        alertasActivas = repository.getAlertasActivasConProducto();
        conteoAlertasActivas = repository.getConteoAlertasActivas();
    }

    public LiveData<List<AlertaConProducto>> getAlertasActivas() {
        return alertasActivas;
    }

    public LiveData<Integer> getConteoAlertasActivas() {
        return conteoAlertasActivas;
    }

    public void verificarAlertasAhora() {
        repository.verificarAlertasAhora();
    }

    public void marcarComoResuelta(int idAlerta) {
        repository.marcarComoResuelta(idAlerta);
    }
}
