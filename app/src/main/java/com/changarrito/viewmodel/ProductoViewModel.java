package com.changarrito.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.repository.ProductoRepository;

import java.util.List;

public class ProductoViewModel extends AndroidViewModel {

    private ProductoRepository repository;
    private LiveData<List<ProductoEntity>> allProductos;
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProductoViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductoRepository(application);
        allProductos = repository.getAllProductos();
    }

    public LiveData<List<ProductoEntity>> getAllProductos() {
        return allProductos;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public boolean guardarProducto(ProductoEntity producto) {
        String error = ProductoValidator.validar(producto);
        if (error != null) {
            errorMessage.setValue(error);
            return false;
        }

        long ahora = System.currentTimeMillis();
        producto.timestampActualizacionMs = ahora;

        if (producto.id == 0) {
            producto.timestampCreacionMs = ahora;
            repository.insert(producto);
        } else {
            repository.update(producto);
        }
        return true;
    }

    public void eliminarProducto(ProductoEntity producto) {
        repository.delete(producto);
    }

    public LiveData<List<ProductoEntity>> buscar(String nombre) {
        return repository.search(nombre);
    }

    public LiveData<ProductoEntity> getProductoByBarcode(String barcode) {
        return repository.getProductoByBarcode(barcode);
    }
}