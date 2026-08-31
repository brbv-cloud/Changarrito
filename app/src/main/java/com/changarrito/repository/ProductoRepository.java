package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductoRepository {

    private ProductoDAO productoDAO;
    private LiveData<List<ProductoEntity>> allProductos;
    private ExecutorService executorService;

    public ProductoRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        productoDAO = db.productoDao();
        allProductos = productoDAO.getAllProductos();
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public LiveData<ProductoEntity> getProductoById(int id) {
        return productoDAO.getProductoById(id);
    }

    public void insert(ProductoEntity producto) {
        executorService.execute(() -> productoDAO.insert(producto));
    }

    public void insertAll(List<ProductoEntity> productos) {
        executorService.execute(() -> productoDAO.insertAll(productos));
    }

    public void update(ProductoEntity producto) {
        executorService.execute(() -> productoDAO.update(producto));
    }

    public void delete(ProductoEntity producto) {
        executorService.execute(() -> productoDAO.delete(producto));
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