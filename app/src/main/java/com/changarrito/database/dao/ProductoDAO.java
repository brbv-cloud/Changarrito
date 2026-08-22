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
}