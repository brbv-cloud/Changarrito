package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.bitacora.CambiosProducto;
import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.BitacoraDAO;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductoRepository {

    private final Application application;
    private final AppDatabase db;
    private final BitacoraDAO bitacoraDAO;
    private ProductoDAO productoDAO;
    private LiveData<List<ProductoEntity>> allProductos;
    private ExecutorService executorService;

    public ProductoRepository(Application application) {
        this.application = application;
        db = AppDatabase.getInstance(application);
        productoDAO = db.productoDao();
        bitacoraDAO = db.bitacoraDao();
        allProductos = productoDAO.getAllProductos();
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public LiveData<ProductoEntity> getProductoById(int id) {
        return productoDAO.getProductoById(id);
    }

    /*
     * Altas, ediciones y bajas hechas a mano quedan en la bitácora (Sprint 7), con
     * quién tenía el turno. El cambio y su entrada se guardan en la misma transacción.
     */

    public void insert(ProductoEntity producto) {
        executorService.execute(() -> db.runInTransaction(() -> {
            producto.id = (int) productoDAO.insert(producto);
            bitacoraDAO.insertAll(CambiosProducto.alCrear(producto, usuarioDelTurno(), System.currentTimeMillis()));
        }));
    }

    public void insertAll(List<ProductoEntity> productos) {
        executorService.execute(() -> productoDAO.insertAll(productos));
    }

    public void update(ProductoEntity producto) {
        executorService.execute(() -> db.runInTransaction(() -> {
            ProductoEntity antes = productoDAO.getProductoByIdSync(producto.id);
            productoDAO.update(producto);
            if (antes != null) {
                bitacoraDAO.insertAll(CambiosProducto.alEditar(antes, producto, usuarioDelTurno(),
                        System.currentTimeMillis()));
            }
        }));
    }

    /**
     * v5: "eliminar" desde la app = borrado suave. El producto desaparece de listas,
     * búsquedas y alertas, pero sus ventas y su historial se conservan.
     */
    public void desactivar(ProductoEntity producto) {
        executorService.execute(() -> db.runInTransaction(() -> {
            long ahora = System.currentTimeMillis();
            productoDAO.desactivar(producto.id, ahora);
            bitacoraDAO.insert(CambiosProducto.alEliminar(producto, usuarioDelTurno(), ahora));
        }));
    }

    /** Quién tiene el turno, si su perfil existe (para no romper la llave foránea). */
    private Integer usuarioDelTurno() {
        Integer id = SesionTurno.getIdUsuario(application);
        if (id == null || db.usuarioDao().getUsuarioByIdSync(id) == null) return null;
        return id;
    }

    public LiveData<List<String>> getCategorias() {
        return productoDAO.getCategorias();
    }

    public LiveData<List<ProductoEntity>> search(String nombre) {
        return productoDAO.searchProductoByName(nombre);
    }

    public LiveData<ProductoEntity> getProductoByBarcode(String barcode) {
        return productoDAO.getProductoByBarcode(barcode);
    }

    public LiveData<List<ProductoEntity>> getProductosProximosAVencer(long ahora, long limite) {
        return productoDAO.getProductosProximosAVencer(ahora, limite);
    }
}