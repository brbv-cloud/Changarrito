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

    /** "Ya lo atendi": queda en el registro con el nombre de quien atiende el turno. */
    public void marcarComoResuelta(int idAlerta) {
        String quien = com.changarrito.negocio.SesionTurno.getNombre(application);
        executorService.execute(() ->
                alertaDAO.marcarAtendida(idAlerta, quien, System.currentTimeMillis()));
    }

    public LiveData<List<AlertaConProducto>> getHistorial() {
        return alertaDAO.getHistorial();
    }

    public void eliminarAlerta(AlertaEntity alerta) {
        executorService.execute(() -> alertaDAO.delete(alerta));
    }

    /**
     * Dispara la verificacion en el hilo de fondo propio de este repository (uno solo, asi
     * dos revisiones seguidas no crean la misma alerta dos veces). PrincipalActivity la
     * llama cada vez que cambia el inventario: al vender, editar, surtir o registrar merma.
     */
    public void verificarAlertasAhora() {
        executorService.execute(this::verificarAlertasSync);
    }

    /**
     * Recorre todos los productos y crea alertas de stock bajo / proximo vencimiento
     * cuando corresponde, evitando duplicar una alerta activa ya existente del mismo
     * tipo para el mismo producto. Debe llamarse desde un hilo de fondo.
     */
    public void verificarAlertasSync() {
        // Lo que ya tiene (o tuvo) existencia deja de ser "pendiente de surtir".
        productoDAO.marcarSurtidos(System.currentTimeMillis());
        List<ProductoEntity> productos = productoDAO.getAllProductosSync();
        if (productos == null) return;

        long ahora = System.currentTimeMillis();
        long limiteVencimiento = ahora + (DIAS_AVISO_VENCIMIENTO * UN_DIA_MS);

        for (ProductoEntity producto : productos) {
            verificarStockBajo(producto, ahora);
            verificarVencimiento(producto, ahora, limiteVencimiento);
        }
    }

    /**
     * Existencia, en dos niveles: "queda poco" (STOCK_BAJO) y "se acabo" (AGOTADO).
     * Cada nivel avisa una sola vez; si el usuario marca la alerta como atendida no se
     * repite mientras siga igual. Pero si de "queda poco" pasa a "se acabo", se avisa
     * otra vez. Al surtir arriba del minimo terminan las dos y el ciclo empieza de nuevo.
     */
    private void verificarStockBajo(ProductoEntity producto, long ahora) {
        if (producto.surtidoMs == 0) {
            // Pendiente de surtir (ej. recien importado del catalogo): no es que se acabo,
            // es que todavia no llega. No se avisa.
            alertaDAO.cerrarDeTipo(producto.id, TipoAlerta.STOCK_BAJO, ahora);
            alertaDAO.cerrarDeTipo(producto.id, TipoAlerta.AGOTADO, ahora);
            return;
        }
        boolean stockBajo = producto.cantidadDisponible <= producto.umbralStockBajo + EPSILON;
        if (!stockBajo) {
            // Ya se surtio: terminan los avisos, y si vuelve a bajar se avisa de nuevo.
            alertaDAO.cerrarDeTipo(producto.id, TipoAlerta.STOCK_BAJO, ahora);
            alertaDAO.cerrarDeTipo(producto.id, TipoAlerta.AGOTADO, ahora);
            return;
        }
        boolean agotado = producto.cantidadDisponible <= EPSILON;
        TipoAlerta tipo = agotado ? TipoAlerta.AGOTADO : TipoAlerta.STOCK_BAJO;
        // Una sola tarjeta de existencia por producto: "se acabo" reemplaza a "queda poco"
        // (y al reves si se surte un poco pero sigue bajo).
        alertaDAO.cerrarDeTipo(producto.id, agotado ? TipoAlerta.STOCK_BAJO : TipoAlerta.AGOTADO, ahora);
        // Ya se aviso de este nivel (aunque el usuario la haya marcado como atendida).
        if (alertaDAO.getAlertaDeTipoSync(producto.id, tipo) != null) return;

        AlertaEntity alerta = new AlertaEntity();
        alerta.idProducto = producto.id;
        alerta.tipoAlerta = tipo;
        alerta.estado = EstadoAlerta.ACTIVA;
        alerta.timestampCreacionMs = ahora;
        long id = alertaDAO.insert(alerta);

        String mensaje = agotado
                ? "Se acabó " + producto.nombre + ". Toca para pedirle al proveedor."
                : "Quedan " + formatearCantidad(producto) + " de " + producto.nombre;
        NotificacionUtil.mostrarNotificacionAlerta(application, (int) id, tipo, producto.nombre, mensaje);
    }

    private void verificarVencimiento(ProductoEntity producto, long ahora, long limite) {
        // fechaCaducidadMs <= 0 significa "sin fecha de caducidad capturada"
        if (producto.fechaCaducidadMs <= 0 || producto.fechaCaducidadMs > limite) {
            // Le quitaron la fecha o le pusieron una lejana (lote nuevo): termina el aviso.
            alertaDAO.cerrarDeTipo(producto.id, TipoAlerta.PROXIMO_VENCIMIENTO, ahora);
            return;
        }
        // Ya se aviso de esta fecha (aunque el usuario la haya marcado como atendida).
        if (alertaDAO.getAlertaDeTipoSync(producto.id, TipoAlerta.PROXIMO_VENCIMIENTO) != null) return;

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
