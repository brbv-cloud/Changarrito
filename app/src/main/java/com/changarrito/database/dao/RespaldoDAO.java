package com.changarrito.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;

import java.util.List;

/**
 * Lectura completa y reemplazo de todas las tablas, para el respaldo en la nube.
 * Todo es sincrono: se llama desde un hilo de fondo (RespaldoRepository / RespaldoWorker).
 * Los inserts conservan el id original de cada fila, asi las relaciones
 * (venta → producto, venta → usuario, etc.) siguen apuntando a lo mismo.
 */
@Dao
public interface RespaldoDAO {

    // ---------- Leer todo ----------

    @Query("SELECT * FROM configuracion_negocio WHERE id = 1")
    ConfiguracionNegocioEntity leerConfiguracion();

    @Query("SELECT * FROM usuario ORDER BY id")
    List<UsuarioEntity> leerUsuarios();

    @Query("SELECT * FROM producto ORDER BY id")
    List<ProductoEntity> leerProductos();

    @Query("SELECT * FROM venta ORDER BY id")
    List<VentaEntity> leerVentas();

    @Query("SELECT * FROM cierre_caja ORDER BY id")
    List<CierreCajaEntity> leerCierres();

    @Query("SELECT * FROM bitacora ORDER BY id")
    List<BitacoraEntity> leerBitacora();

    // ---------- Borrar todo (en orden: primero lo que depende de otras tablas) ----------

    @Query("DELETE FROM alerta")
    void borrarAlertas();

    @Query("DELETE FROM bitacora")
    void borrarBitacora();

    @Query("DELETE FROM venta")
    void borrarVentas();

    @Query("DELETE FROM cierre_caja")
    void borrarCierres();

    @Query("DELETE FROM producto")
    void borrarProductos();

    @Query("DELETE FROM usuario")
    void borrarUsuarios();

    @Query("DELETE FROM configuracion_negocio")
    void borrarConfiguracion();

    // ---------- Insertar con sus ids originales ----------

    @Insert
    void insertarConfiguracion(ConfiguracionNegocioEntity configuracion);

    @Insert
    void insertarUsuarios(List<UsuarioEntity> usuarios);

    @Insert
    void insertarProductos(List<ProductoEntity> productos);

    @Insert
    void insertarVentas(List<VentaEntity> ventas);

    @Insert
    void insertarCierres(List<CierreCajaEntity> cierres);

    @Insert
    void insertarBitacora(List<BitacoraEntity> bitacora);
}
