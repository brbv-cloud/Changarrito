package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.changarrito.database.entity.ProductoEntity;

import java.util.List;

@Dao
public interface ProductoDAO {

    @Insert
    long insert(ProductoEntity producto);

    @Insert
    void insertAll(List<ProductoEntity> productos);

    @Update
    int update(ProductoEntity producto);

    @Delete
    int delete(ProductoEntity producto);

    @Query("SELECT * FROM producto")
    LiveData<List<ProductoEntity>> getAllProductos();

    @Query("SELECT * FROM producto WHERE id = :id")
    LiveData<ProductoEntity> getProductoById(int id);

    @Query("SELECT * FROM producto WHERE nombre LIKE '%' || :nombre || '%'")
    LiveData<List<ProductoEntity>> searchProductoByName(String nombre);

    @Query("SELECT * FROM producto WHERE barcode = :barcode LIMIT 1")
    LiveData<ProductoEntity> getProductoByBarcode(String barcode);

    @Query("SELECT * FROM producto WHERE fechaCaducidadMs BETWEEN :ahora AND :limite")
    LiveData<List<ProductoEntity>> getProductosProximosAVencer(long ahora, long limite);
}