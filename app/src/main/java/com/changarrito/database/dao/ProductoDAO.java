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

    /**
     * Borrado FÍSICO. Solo para tests y mantenimiento: la app nunca lo usa, porque
     * Venta tiene llave foránea a Producto (RESTRICT) y un producto con ventas no se
     * puede borrar. Para "eliminar" desde la app se usa desactivar().
     */
    @Delete
    int delete(ProductoEntity producto);

    /** v5: borrado suave. El producto deja de aparecer en listas y alertas, pero sus ventas lo conservan. */
    @Query("UPDATE producto SET activo = 0, timestampActualizacionMs = :ahoraMs WHERE id = :id")
    int desactivar(int id, long ahoraMs);

    @Query("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre COLLATE NOCASE")
    LiveData<List<ProductoEntity>> getAllProductos();

    /**
     * Version sincrona (sin LiveData) para AlertaRepository.verificarAlertasSync(),
     * que recorre todos los productos en un hilo de fondo (WorkManager o el
     * ExecutorService del repository). Nunca llamar desde el hilo principal.
     */
    @Query("SELECT * FROM producto WHERE activo = 1")
    List<ProductoEntity> getAllProductosSync();

    /** Todos, incluidos los desactivados: para el respaldo en Firestore (las ventas los referencian). */
    @Query("SELECT * FROM producto")
    List<ProductoEntity> getTodosIncluyendoInactivosSync();

    @Query("SELECT DISTINCT categoria FROM producto WHERE activo = 1 AND categoria IS NOT NULL AND categoria != '' ORDER BY categoria COLLATE NOCASE")
    LiveData<List<String>> getCategorias();

    @Query("SELECT * FROM producto WHERE id = :id")
    LiveData<ProductoEntity> getProductoById(int id);

    /**
     * Versión síncrona (sin LiveData) para usar dentro de una transacción en
     * un hilo de fondo, ej. VentaRepository.registrarVenta(). Nunca llamar
     * desde el hilo principal.
     */
    @Query("SELECT * FROM producto WHERE id = :id")
    ProductoEntity getProductoByIdSync(int id);

    @Query("SELECT * FROM producto WHERE activo = 1 AND nombre LIKE '%' || :nombre || '%'")
    LiveData<List<ProductoEntity>> searchProductoByName(String nombre);

    @Query("SELECT * FROM producto WHERE activo = 1 AND barcode = :barcode LIMIT 1")
    LiveData<ProductoEntity> getProductoByBarcode(String barcode);

    @Query("SELECT * FROM producto WHERE activo = 1 AND fechaCaducidadMs BETWEEN :ahora AND :limite")
    LiveData<List<ProductoEntity>> getProductosProximosAVencer(long ahora, long limite);

    /**
     * "Huesos": productos con existencia que no se venden desde antes de limiteMs
     * (o que nunca se han vendido y se dieron de alta antes de esa fecha). Los que
     * llevan mas tiempo quietos van primero.
     */
    String SQL_HUESOS = "SELECT p.id AS idProducto, p.nombre AS nombre, p.cantidadDisponible AS existencia, "
            + "p.unidadMedida AS unidadMedida, p.costoCompra AS costo, p.precio AS precio, "
            + "(SELECT MAX(v.timestampVentaMs) FROM venta v WHERE v.idProducto = p.id) AS ultimaVentaMs, "
            + "p.timestampCreacionMs AS creadoMs "
            + "FROM producto p WHERE p.activo = 1 AND p.cantidadDisponible > 0 "
            + "AND COALESCE((SELECT MAX(v2.timestampVentaMs) FROM venta v2 WHERE v2.idProducto = p.id), "
            + "CASE WHEN p.timestampCreacionMs > 0 THEN p.timestampCreacionMs ELSE :limiteMs END) < :limiteMs "
            + "ORDER BY COALESCE(ultimaVentaMs, creadoMs) ASC";

    @Query(SQL_HUESOS)
    LiveData<List<ProductoHueso>> getHuesos(long limiteMs);

    @Query(SQL_HUESOS)
    List<ProductoHueso> getHuesosSync(long limiteMs);
}