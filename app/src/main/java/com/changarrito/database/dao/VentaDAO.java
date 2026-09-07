package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.VentaEntity;

import java.util.List;

@Dao
public interface VentaDAO {

    @Insert
    long insert(VentaEntity venta);

    @Query("SELECT * FROM venta ORDER BY timestampVentaMs DESC")
    LiveData<List<VentaEntity>> getAllVentas();

    @Query("SELECT * FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs ORDER BY timestampVentaMs DESC")
    LiveData<List<VentaEntity>> getVentasByDate(long inicioMs, long finMs);

    @Query("SELECT COALESCE(SUM(total), 0) FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs")
    LiveData<Double> getTotalVentasPorDia(long inicioMs, long finMs);

    @Query("SELECT p.nombre AS nombre, " +
            "SUM(v.cantidadVendida) AS cantidadVendida, " +
            "v.unidadMedida AS unidadMedida, " +
            "SUM(v.total) AS totalIngresos " +
            "FROM venta v INNER JOIN producto p ON p.id = v.idProducto " +
            "GROUP BY v.idProducto " +
            "ORDER BY SUM(v.total) DESC LIMIT :top")
    LiveData<List<ProductoConTotal>> getProductosMasVendidos(int top);
}
