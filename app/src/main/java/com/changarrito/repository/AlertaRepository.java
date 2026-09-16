package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.database.dao.AlertaDAO;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.AlertaEntity;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.utils.NotificacionUtil;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Genera y administra las alertas de stock bajo y proximo vencimiento.
 *
 * verificarAlertasSync() esta pensado para correr en un hilo de fondo ya existente:
 * lo llama tanto AlertaCheckWorker (WorkManager, cada 24h) como el chequeo manual al
 * abrir la app (via verificarAlertasAhora(), que usa el ExecutorService propio de
 * esta clase). Nunca debe llamarse desde el hilo principal.
 */
public class AlertaRepository {

    private static final double EPSILON = 0.0001;
    private static final long DIAS_AVISO_VENCIMIENTO = 7;
    private static final long UN_DIA_MS = 24L * 60 * 60 * 1000;

    private final AlertaDAO alertaDAO;
    private final ProductoDAO productoDAO;
    private final Application application;
    private final ExecutorService executorService;

    public AlertaRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getInstance(application);
        alertaDAO = db.alertaDao();
        productoDAO = db.productoDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<AlertaConProducto>> getAlertasActivasConProducto() {
        return alertaDAO.getAlertasConProducto(EstadoAlerta.ACTIVA);
    }

    public LiveData<Integer> getConteoAlertasActivas() {
        return alertaDAO.contarPorEstado(EstadoAlerta.ACTIVA);
    }

    public void marcarComoResuelta(int idAlerta) {
        executorService.execute(() ->
                alertaDAO.actualizarEstado(idAlerta, EstadoAlerta.RESUELTA, System.currentTimeMillis()));
    }

    public void eliminarAlerta(AlertaEntity alerta) {
        executorService.execute(() -> alertaDAO.delete(alerta));
    }

    /** Dispara la verificacion en el hilo de fondo propio de este repository. Uso: al abrir la app. */
    public void verificarAlertasAhora() {
        executorService.execute(this::verificarAlertasSync);
    }

    /**
     * Recorre todos los productos y crea alertas de stock bajo / proximo vencimiento
     * cuando corresponde, evitando duplicar una alerta activa ya existente del mismo
     * tipo para el mismo producto. Debe llamarse desde un hilo de fondo.
     */
    public void verificarAlertasSync() {
        List<ProductoEntity> productos = productoDAO.getAllProductosSync();
        if (productos == null) return;

        long ahora = System.currentTimeMillis();
        long limiteVencimiento = ahora + (DIAS_AVISO_VENCIMIENTO * UN_DIA_MS);

        for (ProductoEntity producto : productos) {
            verificarStockBajo(producto, ahora);
            verificarVencimiento(producto, ahora, limiteVencimiento);
        }
    }

    private void verificarStockBajo(ProductoEntity producto, long ahora) {
        boolean stockBajo = producto.cantidadDisponible <= producto.umbralStockBajo + EPSILON;
        if (!stockBajo) return;

        AlertaEntity existente = alertaDAO.getAlertaExistenteSync(
                producto.id, TipoAlerta.STOCK_BAJO, EstadoAlerta.ACTIVA);
        if (existente != null) return;

        AlertaEntity alerta = new AlertaEntity();
        alerta.idProducto = producto.id;
        alerta.tipoAlerta = TipoAlerta.STOCK_BAJO;
        alerta.estado = EstadoAlerta.ACTIVA;
        alerta.timestampCreacionMs = ahora;
        long id = alertaDAO.insert(alerta);

        String mensaje = "Quedan " + formatearCantidad(producto) + " de " + producto.nombre;
        NotificacionUtil.mostrarNotificacionAlerta(
                application, (int) id, TipoAlerta.STOCK_BAJO, producto.nombre, mensaje);
    }

    private void verificarVencimiento(ProductoEntity producto, long ahora, long limite) {
        // fechaCaducidadMs <= 0 significa "sin fecha de caducidad capturada"
        if (producto.fechaCaducidadMs <= 0) return;
        if (producto.fechaCaducidadMs > limite) return;

        AlertaEntity existente = alertaDAO.getAlertaExistenteSync(
                producto.id, TipoAlerta.PROXIMO_VENCIMIENTO, EstadoAlerta.ACTIVA);
        if (existente != null) return;

        AlertaEntity alerta = new AlertaEntity();
        alerta.idProducto = producto.id;
        alerta.tipoAlerta = TipoAlerta.PROXIMO_VENCIMIENTO;
        alerta.estado = EstadoAlerta.ACTIVA;
        alerta.timestampCreacionMs = ahora;
        long id = alertaDAO.insert(alerta);

        long diasRestantes = Math.max(0, (producto.fechaCaducidadMs - ahora) / UN_DIA_MS);
        String mensaje = producto.nombre + " vence en " + diasRestantes + " dia(s)";
        NotificacionUtil.mostrarNotificacionAlerta(
                application, (int) id, TipoAlerta.PROXIMO_VENCIMIENTO, producto.nombre, mensaje);
    }

    private String formatearCantidad(ProductoEntity producto) {
        String unidad = producto.unidadMedida != null ? producto.unidadMedida.name() : "";
        double cantidad = producto.cantidadDisponible;
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.getDefault(), "%.0f %s", cantidad, unidad);
        }
        String texto = String.format(Locale.getDefault(), "%.3f", cantidad)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return texto + " " + unidad;
    }
}
