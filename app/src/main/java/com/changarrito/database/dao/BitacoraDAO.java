package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.BitacoraEntity;

import java.util.List;

/** Solo insertar y consultar: la bitácora no se edita ni se borra. */
@Dao
public interface BitacoraDAO {

    @Insert
    long insert(BitacoraEntity entrada);

    @Insert
    void insertAll(List<BitacoraEntity> entradas);

    @Query("SELECT * FROM bitacora ORDER BY timestampMs DESC LIMIT :limite")
    LiveData<List<BitacoraEntity>> getRecientes(int limite);

    @Query("SELECT * FROM bitacora WHERE idProducto = :idProducto ORDER BY timestampMs DESC")
    LiveData<List<BitacoraEntity>> getPorProducto(int idProducto);

    @Query("SELECT * FROM bitacora WHERE timestampMs BETWEEN :inicioMs AND :finMs ORDER BY timestampMs DESC")
    List<BitacoraEntity> getEnRangoSync(long inicioMs, long finMs);
}
