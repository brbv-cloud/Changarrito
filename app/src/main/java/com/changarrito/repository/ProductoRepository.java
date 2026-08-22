package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

import java.util.List;

public class ProductoRepository {

    private ProductoDAO productoDAO;
    private LiveData<List<ProductoEntity>> allProductos;

    public ProductoRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        productoDAO = db.productoDao();
        allProductos = productoDAO.getAllProductos();
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public LiveData<ProductoEntity> getProductoById(int id) {
        return productoDAO.getProductoById(id);
    }

    public void insert(ProductoEntity producto) {
        new Thread(() -> productoDAO.insert(producto)).start();
    }

    public void update(ProductoEntity producto) {
        new Thread(() -> productoDAO.update(producto)).start();
    }

    public void delete(ProductoEntity producto) {
        new Thread(() -> productoDAO.delete(producto)).start();
    }

    public LiveData<List<ProductoEntity>> search(String nombre) {
        return productoDAO.searchProductoByName(nombre);
    }
}