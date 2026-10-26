package com.changarrito.workers;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.changarrito.respaldo.RespaldoException;
import com.changarrito.respaldo.RespaldoRepository;

import java.util.concurrent.TimeUnit;

/**
 * Respaldo automatico en la nube. Solo corre cuando hay internet (WorkManager espera
 * solo a que haya conexion). Se programa:
 *  - una vez al dia (programarDiario), y
 *  - justo despues de cada cierre de caja (respaldarPronto).
 */
public class RespaldoWorker extends Worker {

    private static final String UNICO_DIARIO = "respaldo_diario";
    private static final String UNICO_PRONTO = "respaldo_pronto";

    public RespaldoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            new RespaldoRepository((Application) getApplicationContext()).respaldarSync();
            return Result.success();
        } catch (RespaldoException e) {
            switch (e.tipo) {
                case SIN_INTERNET:
                case DESCONOCIDO:
                    return Result.retry();
                default:
                    // Sin sesion, sin negocio o sin permiso: reintentar no lo arregla.
                    return Result.success();
            }
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static Constraints conInternet() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    public static void programarDiario(Context c) {
        PeriodicWorkRequest diario = new PeriodicWorkRequest.Builder(RespaldoWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(conInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(UNICO_DIARIO, ExistingPeriodicWorkPolicy.KEEP, diario);
    }

    /** Respaldo en cuanto haya internet (ej. despues de cerrar caja). */
    public static void respaldarPronto(Context c) {
        OneTimeWorkRequest pronto = new OneTimeWorkRequest.Builder(RespaldoWorker.class)
                .setConstraints(conInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(c).enqueueUniqueWork(UNICO_PRONTO, ExistingWorkPolicy.REPLACE, pronto);
    }

    /** Al cerrar sesion: que no intente respaldar sin cuenta. */
    public static void cancelar(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(UNICO_DIARIO);
        WorkManager.getInstance(c).cancelUniqueWork(UNICO_PRONTO);
    }
}
