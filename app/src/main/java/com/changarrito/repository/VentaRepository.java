package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.database.dao.ProductoConTotal;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.VentaEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VentaRepository {

    /**
     * Tolerancia para comparar doubles (cantidades). Nunca comparar cantidades con
     * == directo: errores de redondeo de punto flotante (ej. 0.1 + 0.2 != 0.3)
     * pueden rechazar una venta válida o dejar pasar una que no debía.
     */
    private static final double EPSILON = 0.0001;

    private final AppDatabase db;
    private final VentaDAO ventaDAO;
    private final ProductoDAO productoDAO;
    private final LiveData<List<VentaEntity>> allVentas;
    private final ExecutorService executorService;

    private final Application application;

    public VentaRepository(Application application) {
        this.application = application;
        db = AppDatabase.getInstance(application);
        ventaDAO = db.ventaDao();
        productoDAO = db.productoDao();
        allVentas = ventaDAO.getAllVentas();
        executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * Id de quien tiene el turno abierto, o null. Se confirma que el perfil exista en
     * la BD para no romper la llave foránea (p. ej. en tests con una BD en memoria).
     * Llamar dentro de la transacción, en el hilo de fondo.
     */
    private Integer usuarioDelTurno() {
        Integer id = SesionTurno.getIdUsuario(application);
        if (id == null || db.usuarioDao().getUsuarioByIdSync(id) == null) return null;
        return id;
    }

    public LiveData<List<VentaEntity>> getAllVentas() {
        return allVentas;
    }

    public LiveData<List<VentaEntity>> getVentasByDate(long inicioMs, long finMs) {
        return ventaDAO.getVentasByDate(inicioMs, finMs);
    }

    public LiveData<Double> getTotalVentasPorDia(long inicioMs, long finMs) {
        return ventaDAO.getTotalVentasPorDia(inicioMs, finMs);
    }

    public LiveData<List<ProductoConTotal>> getProductosMasVendidos(int top) {
        return ventaDAO.getProductosMasVendidos(top);
    }

    /**
     * $50 deseados / $precio por unidad = cuántas unidades (KG/GR/M) le corresponden
     * al cliente. Usa el precio en memoria del producto ya cargado en la UI: no
     * necesita ir a la BD.
     */
    public double calcularCantidadPorImporte(ProductoEntity producto, double importeDeseado) {
        if (producto == null || producto.precio <= 0) return 0;
        return importeDeseado / producto.precio;
    }

    public interface RegistrarVentaCallback {
        void onResultado(ResultadoVenta resultado);
    }

    /**
     * Registra una venta de un solo producto. Se ejecuta en un hilo de fondo y el
     * callback llega en ese mismo hilo de fondo (el llamador decide si necesita
     * saltar al hilo principal para tocar UI).
     */
    public void registrarVenta(int idProducto, double cantidadVendida, RegistrarVentaCallback callback) {
        executorService.execute(() -> callback.onResultado(registrarVentaSync(idProducto, cantidadVendida)));
    }

    /**
     * Toda la validación + el descuento de inventario + el insert de la Venta pasan
     * dentro de una sola transacción (@Transaction vía db.runInTransaction): o se
     * aplican los dos cambios juntos, o ninguno. Evita que una venta quede
     * registrada sin descontar inventario (o viceversa) si algo falla a la mitad.
     */
    private ResultadoVenta registrarVentaSync(int idProducto, double cantidadVendida) {
        final ResultadoVenta[] resultado = new ResultadoVenta[1];

        db.runInTransaction(() -> {
            ProductoEntity producto = productoDAO.getProductoByIdSync(idProducto);

            if (producto == null) {
                resultado[0] = ResultadoVenta.error("El producto ya no existe.");
                return;
            }
            if (!producto.activo) {
                resultado[0] = ResultadoVenta.error(producto.nombre + " ya no está en el inventario.");
                return;
            }
            if (cantidadVendida <= 0) {
                resultado[0] = ResultadoVenta.error("La cantidad debe ser mayor a 0.");
                return;
            }
            if (producto.unidadMedida == UnidadMedida.PZA
                    && Math.abs(cantidadVendida - Math.round(cantidadVendida)) > EPSILON) {
                resultado[0] = ResultadoVenta.error(
                        producto.nombre + " se vende por pieza, no acepta decimales.");
                return;
            }
            if (cantidadVendida > producto.cantidadDisponible + EPSILON) {
                resultado[0] = ResultadoVenta.error(
                        "No hay suficiente stock de " + producto.nombre + ".");
                return;
            }

            double nuevaCantidad = producto.cantidadDisponible - cantidadVendida;
            if (Math.abs(nuevaCantidad) < EPSILON) {
                nuevaCantidad = 0; // evita dejar restos como -0.0000000002 por redondeo
            }
            producto.cantidadDisponible = nuevaCantidad;
            producto.timestampActualizacionMs = System.currentTimeMillis();
            productoDAO.update(producto);

            VentaEntity venta = new VentaEntity();
            venta.idProducto = producto.id;
            venta.cantidadVendida = cantidadVendida;
            venta.unidadMedida = producto.unidadMedida;
            venta.precioUnitarioVenta = producto.precio;
            venta.costoUnitarioVenta = producto.costoCompra; // v5: base para reportar ganancias
            venta.total = producto.precio * cantidadVendida;
            venta.timestampVentaMs = System.currentTimeMillis();
            venta.idUsuario = usuarioDelTurno(); // quién vendió (dueño o trabajador)
            ventaDAO.insert(venta);

            resultado[0] = ResultadoVenta.ok();
        });

        return resultado[0];
    }
}
