package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.CierreCajaEntity;

import java.util.List;

@Dao
public interface CierreCajaDAO {

    @Insert
    long insert(CierreCajaEntity cierre);

    @Query("SELECT * FROM cierre_caja WHERE fechaMs = :fechaMs LIMIT 1")
    LiveData<CierreCajaEntity> getCierreByFecha(long fechaMs);

    /** Sincrona: usada por CierreCajaRepository para saber si ya se hizo el cierre de hoy antes de insertar. */
    @Query("SELECT * FROM cierre_caja WHERE fechaMs = :fechaMs LIMIT 1")
    CierreCajaEntity getCierreByFechaSync(long fechaMs);

    @Query("SELECT * FROM cierre_caja ORDER BY fechaMs DESC")
    LiveData<List<CierreCajaEntity>> getHistorialCierres();

    /** El cierre mas reciente (de cualquier dia): marca desde donde contar el "efectivo pendiente". */
    @Query("SELECT * FROM cierre_caja ORDER BY timestampCreacionMs DESC LIMIT 1")
    CierreCajaEntity getUltimoCierreSync();
}
