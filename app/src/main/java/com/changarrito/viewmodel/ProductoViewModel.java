package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;

import java.util.List;

public class ProductoViewModel extends AndroidViewModel {

    private ProductoDAO productoDAO;
    private LiveData<List<ProductoEntity>> allProductos;

    public ProductoViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        productoDAO = db.productoDao();
        allProductos = productoDAO.getAllProductos();
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public void insertarProducto(ProductoEntity producto) {
        new Thread(() -> {
            productoDAO.insert(producto);
        }).start();
    }

    public void actualizarProducto(ProductoEntity producto) {
        new Thread(() -> {
            productoDAO.update(producto);
        }).start();
    }

    public void eliminarProducto(ProductoEntity producto) {
        new Thread(() -> {
            productoDAO.delete(producto);
        }).start();
    }
}