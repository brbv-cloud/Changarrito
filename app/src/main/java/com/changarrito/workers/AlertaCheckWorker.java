package com.changarrito.workers;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.changarrito.repository.AlertaRepository;

/**
 * Corre en segundo plano (WorkManager) cada 24h para revisar stock bajo y productos
 * proximos a vencer, sin depender de que el usuario tenga la app abierta.
 *
 * NO usa Kotlin Coroutines (proyecto 100% Java): doWork() ya corre en un hilo de
 * fondo propio de WorkManager, no hace falta un ExecutorService adicional aqui.
 */
public class AlertaCheckWorker extends Worker {

    public AlertaCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AlertaRepository repository = new AlertaRepository((Application) getApplicationContext());
            repository.verificarAlertasSync();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
