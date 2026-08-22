package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.repository.ProductoRepository;

import java.util.List;

public class ProductoViewModel extends AndroidViewModel {

    private ProductoRepository repository;
    private LiveData<List<ProductoEntity>> allProductos;

    public ProductoViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductoRepository(application);
        allProductos = repository.getAllProductos();
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public void insertarProducto(ProductoEntity producto) {
        repository.insert(producto);
    }

    public void actualizarProducto(ProductoEntity producto) {
        repository.update(producto);
    }

    public void eliminarProducto(ProductoEntity producto) {
        repository.delete(producto);
    }

    public LiveData<List<ProductoEntity>> buscar(String nombre) {
        return repository.search(nombre);
    }
}