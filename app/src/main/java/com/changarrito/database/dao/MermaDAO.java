package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.MermaEntity;

import java.util.List;

@Dao
public interface MermaDAO {

    String SQL_POR_MOTIVO = "SELECT motivo, COUNT(*) AS registros, "
            + "COALESCE(SUM(cantidad * costoUnitario), 0) AS costo "
            + "FROM merma WHERE timestampMs BETWEEN :inicioMs AND :finMs "
            + "GROUP BY motivo ORDER BY costo DESC";

    String SQL_EN_RANGO = "SELECT m.*, u.nombre AS nombreUsuario FROM merma m "
            + "LEFT JOIN usuario u ON u.id = m.idUsuario "
            + "WHERE m.timestampMs BETWEEN :inicioMs AND :finMs "
            + "ORDER BY m.timestampMs DESC";

    @Insert
    long insert(MermaEntity merma);

    @Query(SQL_POR_MOTIVO)
    LiveData<List<MermaPorMotivo>> getPorMotivo(long inicioMs, long finMs);

    @Query(SQL_POR_MOTIVO)
    List<MermaPorMotivo> getPorMotivoSync(long inicioMs, long finMs);

    @Query(SQL_EN_RANGO)
    LiveData<List<MermaConUsuario>> getEnRango(long inicioMs, long finMs);

    @Query(SQL_EN_RANGO)
    List<MermaConUsuario> getEnRangoSync(long inicioMs, long finMs);
}
