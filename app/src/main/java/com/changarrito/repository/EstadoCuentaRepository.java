package com.changarrito.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierreCajaDAO;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.pdf.GeneradorEstadoCuenta;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.DatosEstadoCuenta;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.reportes.SerieDiaria;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Junta los datos de un periodo y genera el PDF "Estado de cuenta" en un hilo de fondo. */
public class EstadoCuentaRepository {

    public interface Callback {
        void onListo(File pdf, int paginas);

        void onError(String mensaje);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Application app;
    private final Handler principal = new Handler(Looper.getMainLooper());

    public EstadoCuentaRepository(Application app) {
        this.app = app;
    }

    public void generar(RangoReporte rango, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                DatosEstadoCuenta datos = cargarSync(rango, System.currentTimeMillis());
                GeneradorEstadoCuenta generador = new GeneradorEstadoCuenta(app);
                File pdf = generador.generar(datos);
                int paginas = generador.getPaginas();
                principal.post(() -> callback.onListo(pdf, paginas));
            } catch (Throwable e) {
                Log.e("EstadoCuenta", "No se pudo generar el PDF", e);
                principal.post(() -> callback.onError(e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : "")));
            }
        });
    }

    /** Sincrono (hilo de fondo). Publico para poder probarlo con una BD en memoria. */
    public DatosEstadoCuenta cargarSync(RangoReporte rango, long ahoraMs) {
        AppDatabase db = AppDatabase.getInstance(app);
        VentaDAO ventas = db.ventaDao();
        CierreCajaDAO cierres = db.cierreCajaDao();
        long[] r = rango.calcular(ahoraMs);

        DatosEstadoCuenta d = new DatosEstadoCuenta();
        d.rango = rango;
        d.inicioMs = r[0];
        d.finMs = r[1];
        d.generadoMs = ahoraMs;
        d.configuracion = db.configuracionNegocioDao().getConfiguracionSync();
        ResumenGanancia resumen = ventas.getResumenGananciaSync(r[0], r[1]);
        if (resumen != null) d.resumen = resumen;
        d.dias = SerieDiaria.completar(ventas.getGananciaPorDiaSync(r[0], r[1]), r[0], r[1]);
        d.productos = ventas.getGananciaPorProductoSync(r[0], r[1]);
        d.personas = CalculoGanancias.combinarPersonas(
                ventas.getVentasPorUsuarioSync(r[0], r[1]), cierres.getCierresPorUsuarioSync(r[0], r[1]));
        d.cierres = cierres.getCierresEnRangoSync(r[0], r[1]);
        d.porHora = ventas.getVentasPorHoraSync(r[0], r[1]);
        d.porCategoria = ventas.getVentasPorCategoriaSync(r[0], r[1]);
        d.huesos = db.productoDao().getHuesosSync(ahoraMs - DatosEstadoCuenta.DIAS_HUESO * 24L * 60 * 60 * 1000);
        d.mermasPorMotivo = db.mermaDao().getPorMotivoSync(r[0], r[1]);
        d.mermas = db.mermaDao().getEnRangoSync(r[0], r[1]);
        return d;
    }
}
