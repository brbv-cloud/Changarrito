package com.changarrito.workers;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.changarrito.negocio.CambioDeNegocio;
import com.changarrito.utils.Conexion;
import com.changarrito.utils.NotificacionUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Al final del dia (9 de la noche): si hay ventas o cambios que todavia no estan en la
 * nube, se manda a respaldar. Con internet se respalda en ese momento y avisa "listo";
 * sin internet avisa que falta y se respalda solo en cuanto haya conexion.
 * Si todo ya estaba respaldado no manda nada (para no molestar).
 */
public class RecordatorioRespaldoWorker extends Worker {

    static final String UNICO = "recordatorio_respaldo";
    private static final int HORA = 21;

    public RecordatorioRespaldoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Application app = (Application) getApplicationContext();
            if (FirebaseAuth.getInstance().getCurrentUser() == null) return Result.success();
            CambioDeNegocio.Resumen r = CambioDeNegocio.leerSync(app);
            if (r.nombreNegocio == null || !r.hayCambiosSinRespaldar()) return Result.success();
            // Espera a que haya internet y, al terminar, avisa que ya quedo.
            RespaldoWorker.respaldarPronto(app, true);
            if (!Conexion.hayInternet(app)) NotificacionUtil.mostrarRespaldoPendiente(app);
        } catch (Exception e) {
            // Un recordatorio que falla no debe romper nada: manana lo vuelve a intentar.
        }
        return Result.success();
    }

    /** Una vez al dia, a las 9 de la noche. */
    public static void programar(Context c) {
        PeriodicWorkRequest diario = new PeriodicWorkRequest.Builder(RecordatorioRespaldoWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(msHastaLas(HORA, System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(UNICO, ExistingPeriodicWorkPolicy.KEEP, diario);
    }

    /**
     * Solo para probar (version de desarrollo): corre el recordatorio una vez dentro de
     * unos segundos, como si ya fueran las 9 de la noche. No cambia el de todos los dias.
     */
    public static void probarPronto(Context c, int segundos) {
        OneTimeWorkRequest prueba = new OneTimeWorkRequest.Builder(RecordatorioRespaldoWorker.class)
                .setInitialDelay(segundos, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(c).enqueueUniqueWork(UNICO + "_prueba", ExistingWorkPolicy.REPLACE, prueba);
    }

    /** Cuanto falta para la siguiente vez que den las HORA:00 (hoy o manana). */
    static long msHastaLas(int hora, long ahora) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ahora);
        c.set(Calendar.HOUR_OF_DAY, hora);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= ahora) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis() - ahora;
    }
}
