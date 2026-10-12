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

    @Query("SELECT * FROM cierre_caja ORDER BY timestampCreacionMs DESC")
    LiveData<List<CierreCajaEntity>> getHistorialCierres();

    /** El cierre mas reciente (de cualquier dia): marca desde donde contar el "efectivo pendiente". */
    @Query("SELECT * FROM cierre_caja ORDER BY timestampCreacionMs DESC LIMIT 1")
    CierreCajaEntity getUltimoCierreSync();

    /** Cierres por persona en un rango: cuantos hizo y cuanto falto/sobro en total. */
    @Query("SELECT c.idUsuario AS idUsuario, u.nombre AS nombre, COUNT(*) AS cierres, " +
            "COALESCE(SUM(CASE WHEN c.diferencia < -0.005 THEN c.diferencia ELSE 0 END), 0) AS faltante, " +
            "COALESCE(SUM(CASE WHEN c.diferencia > 0.005 THEN c.diferencia ELSE 0 END), 0) AS sobrante " +
            "FROM cierre_caja c LEFT JOIN usuario u ON u.id = c.idUsuario " +
            "WHERE c.timestampCreacionMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY c.idUsuario")
    LiveData<List<CierresPorUsuario>> getCierresPorUsuario(long inicioMs, long finMs);
}
