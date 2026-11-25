package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.GastoEntity;

import java.util.List;

@Dao
public interface GastoDAO {

    String SQL_POR_TIPO = "SELECT tipo, COUNT(*) AS registros, COALESCE(SUM(monto), 0) AS total "
            + "FROM gasto WHERE timestampMs BETWEEN :inicioMs AND :finMs "
            + "GROUP BY tipo ORDER BY total DESC";

    String SQL_EN_RANGO = "SELECT g.*, u.nombre AS nombreUsuario FROM gasto g "
            + "LEFT JOIN usuario u ON u.id = g.idUsuario "
            + "WHERE g.timestampMs BETWEEN :inicioMs AND :finMs "
            + "ORDER BY g.timestampMs DESC";

    @Insert
    long insert(GastoEntity gasto);

    @Query("SELECT * FROM gasto WHERE id = :id")
    GastoEntity getByIdSync(int id);

    @Query("DELETE FROM gasto WHERE id = :id")
    int borrar(int id);

    @Query(SQL_POR_TIPO)
    LiveData<List<GastoPorTipo>> getPorTipo(long inicioMs, long finMs);

    @Query(SQL_POR_TIPO)
    List<GastoPorTipo> getPorTipoSync(long inicioMs, long finMs);

    @Query(SQL_EN_RANGO)
    LiveData<List<GastoConUsuario>> getEnRango(long inicioMs, long finMs);

    @Query(SQL_EN_RANGO)
    List<GastoConUsuario> getEnRangoSync(long inicioMs, long finMs);

    /** Lo que salio del cajon desde un momento (el ultimo cierre): se resta del efectivo esperado. */
    @Query("SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE pagadoConCaja = 1 AND timestampMs >= :desdeMs")
    double getTotalCajaDesdeSync(long desdeMs);
}
