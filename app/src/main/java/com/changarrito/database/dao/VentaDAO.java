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

    /**
     * Version sincrona de getTotalVentasPorDia, para usarse dentro de
     * CierreCajaRepository.registrarCierre() en un hilo de fondo (dentro de la
     * transaccion que calcula efectivoSistema). Nunca llamar desde el hilo principal.
     */
    @Query("SELECT COALESCE(SUM(total), 0) FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs")
    double getTotalVentasPorDiaSync(long inicioMs, long finMs);

    @Query("SELECT COUNT(*) FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs")
    LiveData<Integer> getCountVentasEnRango(long inicioMs, long finMs);

    /**
     * Total de ventas estrictamente despues de un timestamp, sin limite superior.
     * La usa CierreCajaRepository para calcular "efectivo pendiente de cierre":
     * lo vendido desde el ultimo cierre (o desde el inicio del dia si todavia no
     * se ha hecho ninguno), no el dia calendario completo. Asi, un segundo cierre
     * el mismo dia no vuelve a contar lo que ya se cerro antes.
     */
    @Query("SELECT COALESCE(SUM(total), 0) FROM venta WHERE timestampVentaMs > :desdeMs")
    double getTotalVentasDesdeSync(long desdeMs);

    @Query("SELECT p.nombre AS nombre, " +
            "SUM(v.cantidadVendida) AS cantidadVendida, " +
            "v.unidadMedida AS unidadMedida, " +
            "SUM(v.total) AS totalIngresos " +
            "FROM venta v INNER JOIN producto p ON p.id = v.idProducto " +
            "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY v.idProducto " +
            "ORDER BY SUM(v.total) DESC LIMIT 1")
    LiveData<ProductoConTotal> getProductoMasVendidoEnRango(long inicioMs, long finMs);

    @Query("SELECT p.nombre AS nombre, " +
            "SUM(v.cantidadVendida) AS cantidadVendida, " +
            "v.unidadMedida AS unidadMedida, " +
            "SUM(v.total) AS totalIngresos " +
            "FROM venta v INNER JOIN producto p ON p.id = v.idProducto " +
            "GROUP BY v.idProducto " +
            "ORDER BY SUM(v.total) DESC LIMIT :top")
    LiveData<List<ProductoConTotal>> getProductosMasVendidos(int top);

    /**
     * Ingresos agrupados por dia (zona horaria local) dentro de un rango, para la
     * grafica de "ultimos N dias". Solo trae dias CON al menos una venta: los dias
     * sin ventas se rellenan con 0 en ReportesRepository/ReportesActivity, no aqui.
     */
    @Query("SELECT strftime('%Y-%m-%d', timestampVentaMs / 1000, 'unixepoch', 'localtime') AS fecha, " +
            "COALESCE(SUM(total), 0) AS total " +
            "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY fecha ORDER BY fecha ASC")
    LiveData<List<IngresoDia>> getIngresosPorDiaEnRango(long inicioMs, long finMs);

    // ---------- Ganancias (solo dueno) ----------
    // Una venta "tiene costo" si costoUnitarioVenta > 0: se guardo el costo del
    // producto al momento de vender. Las que no, se reportan aparte.

    @Query("SELECT COALESCE(SUM(total), 0) AS ventas, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN total ELSE 0 END), 0) AS ventasConCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN costoUnitarioVenta * cantidadVendida ELSE 0 END), 0) AS costo, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN 0 ELSE 1 END), 0) AS ventasSinCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN 0 ELSE total END), 0) AS importeSinCosto, " +
            "COUNT(*) AS transacciones, COUNT(DISTINCT idTicket) AS tickets " +
            "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs")
    LiveData<ResumenGanancia> getResumenGanancia(long inicioMs, long finMs);

    /** Version sincrona para el PDF (hilo de fondo). */
    @Query("SELECT COALESCE(SUM(total), 0) AS ventas, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN total ELSE 0 END), 0) AS ventasConCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN costoUnitarioVenta * cantidadVendida ELSE 0 END), 0) AS costo, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN 0 ELSE 1 END), 0) AS ventasSinCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN 0 ELSE total END), 0) AS importeSinCosto, " +
            "COUNT(*) AS transacciones, COUNT(DISTINCT idTicket) AS tickets " +
            "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs")
    ResumenGanancia getResumenGananciaSync(long inicioMs, long finMs);

    @Query("SELECT v.idProducto AS idProducto, p.nombre AS nombre, v.unidadMedida AS unidadMedida, " +
            "SUM(v.cantidadVendida) AS cantidad, SUM(v.total) AS ventas, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.total ELSE 0 END) AS ventasConCosto, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.costoUnitarioVenta * v.cantidadVendida ELSE 0 END) AS costo, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN 0 ELSE 1 END) AS ventasSinCosto " +
            "FROM venta v INNER JOIN producto p ON p.id = v.idProducto " +
            "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY v.idProducto " +
            "ORDER BY (SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.total ELSE 0 END) " +
            "- SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.costoUnitarioVenta * v.cantidadVendida ELSE 0 END)) DESC, " +
            "SUM(v.total) DESC")
    LiveData<List<GananciaProducto>> getGananciaPorProducto(long inicioMs, long finMs);

    /** Version sincrona para el PDF (hilo de fondo). */
    @Query("SELECT v.idProducto AS idProducto, p.nombre AS nombre, v.unidadMedida AS unidadMedida, " +
            "SUM(v.cantidadVendida) AS cantidad, SUM(v.total) AS ventas, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.total ELSE 0 END) AS ventasConCosto, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.costoUnitarioVenta * v.cantidadVendida ELSE 0 END) AS costo, " +
            "SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN 0 ELSE 1 END) AS ventasSinCosto " +
            "FROM venta v INNER JOIN producto p ON p.id = v.idProducto " +
            "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY v.idProducto " +
            "ORDER BY (SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.total ELSE 0 END) " +
            "- SUM(CASE WHEN v.costoUnitarioVenta > 0 THEN v.costoUnitarioVenta * v.cantidadVendida ELSE 0 END)) DESC, " +
            "SUM(v.total) DESC")
    List<GananciaProducto> getGananciaPorProductoSync(long inicioMs, long finMs);

    @Query("SELECT v.idUsuario AS idUsuario, u.nombre AS nombre, " +
            "COUNT(*) AS transacciones, SUM(v.total) AS ventas " +
            "FROM venta v LEFT JOIN usuario u ON u.id = v.idUsuario " +
            "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY v.idUsuario ORDER BY SUM(v.total) DESC")
    LiveData<List<VentasPorUsuario>> getVentasPorUsuario(long inicioMs, long finMs);

    /** Version sincrona para el PDF (hilo de fondo). */
    @Query("SELECT v.idUsuario AS idUsuario, u.nombre AS nombre, " +
            "COUNT(*) AS transacciones, SUM(v.total) AS ventas " +
            "FROM venta v LEFT JOIN usuario u ON u.id = v.idUsuario " +
            "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY v.idUsuario ORDER BY SUM(v.total) DESC")
    List<VentasPorUsuario> getVentasPorUsuarioSync(long inicioMs, long finMs);

    /** Ventas, costo y ganancia por dia (fecha local). Solo dias con ventas; SerieDiaria rellena el resto. */
    @Query("SELECT strftime('%Y-%m-%d', timestampVentaMs / 1000, 'unixepoch', 'localtime') AS fecha, " +
            "COALESCE(SUM(total), 0) AS ventas, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN total ELSE 0 END), 0) AS ventasConCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN costoUnitarioVenta * cantidadVendida ELSE 0 END), 0) AS costo " +
            "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY fecha ORDER BY fecha ASC")
    LiveData<List<GananciaDia>> getGananciaPorDia(long inicioMs, long finMs);

    @Query("SELECT strftime('%Y-%m-%d', timestampVentaMs / 1000, 'unixepoch', 'localtime') AS fecha, " +
            "COALESCE(SUM(total), 0) AS ventas, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN total ELSE 0 END), 0) AS ventasConCosto, " +
            "COALESCE(SUM(CASE WHEN costoUnitarioVenta > 0 THEN costoUnitarioVenta * cantidadVendida ELSE 0 END), 0) AS costo " +
            "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs " +
            "GROUP BY fecha ORDER BY fecha ASC")
    List<GananciaDia> getGananciaPorDiaSync(long inicioMs, long finMs);

    // ---------- v6: hora pico y categorias ----------

    String SQL_POR_HORA = "SELECT CAST(strftime('%H', timestampVentaMs / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hora, "
            + "COUNT(DISTINCT idTicket) AS tickets, COALESCE(SUM(total), 0) AS ventas "
            + "FROM venta WHERE timestampVentaMs BETWEEN :inicioMs AND :finMs "
            + "GROUP BY hora ORDER BY hora";

    String SQL_POR_CATEGORIA = "SELECT CASE WHEN p.categoria IS NULL OR TRIM(p.categoria) = '' "
            + "THEN 'Sin categoría' ELSE TRIM(p.categoria) END AS categoria, "
            + "COALESCE(SUM(v.total), 0) AS ventas "
            + "FROM venta v INNER JOIN producto p ON p.id = v.idProducto "
            + "WHERE v.timestampVentaMs BETWEEN :inicioMs AND :finMs "
            + "GROUP BY 1 ORDER BY ventas DESC";

    @Query(SQL_POR_HORA)
    LiveData<List<VentasPorHora>> getVentasPorHora(long inicioMs, long finMs);

    @Query(SQL_POR_HORA)
    List<VentasPorHora> getVentasPorHoraSync(long inicioMs, long finMs);

    @Query(SQL_POR_CATEGORIA)
    LiveData<List<VentasPorCategoria>> getVentasPorCategoria(long inicioMs, long finMs);

    @Query(SQL_POR_CATEGORIA)
    List<VentasPorCategoria> getVentasPorCategoriaSync(long inicioMs, long finMs);
}
