package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.repository.ResultadoVenta;
import com.changarrito.repository.VentaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Prueba VentaRepository.registrarVenta(...) contra una BD Room EN MEMORIA, inyectada
 * vía AppDatabase.setInstanceParaTest(...) para no tocar la BD real del dispositivo
 * (VentaRepository usa AppDatabase.getInstance(application) internamente, que es un
 * singleton por proceso).
 */
@RunWith(AndroidJUnit4.class)
public class VentaRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ProductoDAO productoDao;
    private VentaRepository ventaRepository;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceParaTest(db);

        productoDao = db.productoDao();
        ventaRepository = new VentaRepository((Application) context);
    }

    @After
    public void cerrarBD() {
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    // ---------- helpers ----------

    private ProductoEntity nuevoProducto(String nombre, double precio, double cantidad, UnidadMedida unidad) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = precio;
        p.cantidadDisponible = cantidad;
        p.unidadMedida = unidad;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "3312345678";
        p.barcode = "";
        p.timestampCreacionMs = System.currentTimeMillis();
        p.timestampActualizacionMs = System.currentTimeMillis();
        return p;
    }

    private ResultadoVenta registrarYEsperar(int idProducto, double cantidad) throws InterruptedException {
        final ResultadoVenta[] resultado = new ResultadoVenta[1];
        CountDownLatch latch = new CountDownLatch(1);
        ventaRepository.registrarVenta(idProducto, cantidad, r -> {
            resultado[0] = r;
            latch.countDown();
        });
        assertTrue("timeout esperando registrarVenta", latch.await(2, TimeUnit.SECONDS));
        return resultado[0];
    }

    private <T> T getValor(LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T valor) {
                data[0] = valor;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        //noinspection unchecked
        return (T) data[0];
    }

    // ---------- tests ----------

    @Test
    public void venta_descuenta_inventario() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Coca Cola 600ml", 18.0, 10, UnidadMedida.PZA));

        ResultadoVenta resultado = registrarYEsperar(id, 3);

        assertTrue(resultado.exito);
        ProductoEntity actualizado = productoDao.getProductoByIdSync(id);
        assertEquals(7.0, actualizado.cantidadDisponible, 0.0001);
    }

    @Test
    public void vender_mas_de_lo_disponible_falla_y_no_toca_inventario() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Leche 1L", 22.0, 5, UnidadMedida.PZA));

        ResultadoVenta resultado = registrarYEsperar(id, 10);

        assertFalse(resultado.exito);
        assertNotNull(resultado.error);
        ProductoEntity sinCambios = productoDao.getProductoByIdSync(id);
        assertEquals(5.0, sinCambios.cantidadDisponible, 0.0001);
    }

    @Test
    public void vender_todo_el_stock_deja_cantidad_en_cero() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Pan", 5.0, 4, UnidadMedida.PZA));

        ResultadoVenta resultado = registrarYEsperar(id, 4);

        assertTrue(resultado.exito);
        ProductoEntity actualizado = productoDao.getProductoByIdSync(id);
        assertEquals(0.0, actualizado.cantidadDisponible, 0.0001);
    }

    @Test
    public void vender_con_stock_en_cero_falla() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Atun", 15.0, 0, UnidadMedida.PZA));

        ResultadoVenta resultado = registrarYEsperar(id, 1);

        assertFalse(resultado.exito);
    }

    @Test
    public void venta_granel_resta_cantidad_decimal_exacta() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Jamón", 120.0, 2.0, UnidadMedida.KG));

        ResultadoVenta resultado = registrarYEsperar(id, 0.750);

        assertTrue(resultado.exito);
        ProductoEntity actualizado = productoDao.getProductoByIdSync(id);
        assertEquals(1.250, actualizado.cantidadDisponible, 0.0001);
    }

    @Test
    public void vender_fraccion_de_pieza_falla() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Huevo (pieza)", 3.0, 30, UnidadMedida.PZA));

        ResultadoVenta resultado = registrarYEsperar(id, 2.5);

        assertFalse(resultado.exito);
        ProductoEntity sinCambios = productoDao.getProductoByIdSync(id);
        assertEquals(30.0, sinCambios.cantidadDisponible, 0.0001);
    }

    @Test
    public void vender_cantidad_negativa_o_cero_falla() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Sal", 10.0, 5, UnidadMedida.KG));

        assertFalse(registrarYEsperar(id, 0).exito);
        assertFalse(registrarYEsperar(id, -1).exito);
    }

    @Test
    public void vender_producto_inexistente_falla() throws Exception {
        ResultadoVenta resultado = registrarYEsperar(9999, 1);
        assertFalse(resultado.exito);
    }

    @Test
    public void venta_exitosa_crea_registro_con_total_correcto() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Aceite 1L", 45.0, 10, UnidadMedida.PZA));

        registrarYEsperar(id, 3);

        List<VentaEntity> ventas = getValor(db.ventaDao().getAllVentas());
        assertEquals(1, ventas.size());
        assertEquals(135.0, ventas.get(0).total, 0.001);
        assertEquals(id, ventas.get(0).idProducto);
    }

    @Test
    public void venta_copia_unidad_de_medida_del_producto() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Frijol", 28.0, 10, UnidadMedida.KG));

        registrarYEsperar(id, 1.5);

        List<VentaEntity> ventas = getValor(db.ventaDao().getAllVentas());
        assertEquals(UnidadMedida.KG, ventas.get(0).unidadMedida);
    }

    @Test
    public void dos_ventas_seguidas_descuentan_acumulativamente() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Arroz", 20.0, 10, UnidadMedida.KG));

        registrarYEsperar(id, 2.0);
        registrarYEsperar(id, 1.5);

        ProductoEntity actualizado = productoDao.getProductoByIdSync(id);
        assertEquals(6.5, actualizado.cantidadDisponible, 0.0001);

        List<VentaEntity> ventas = getValor(db.ventaDao().getAllVentas());
        assertEquals(2, ventas.size());
    }

    @Test
    public void calcular_cantidad_por_importe_da_resultado_correcto() {
        ProductoEntity producto = nuevoProducto("Jamón", 100.0, 5, UnidadMedida.KG);

        double cantidad = ventaRepository.calcularCantidadPorImporte(producto, 50.0);

        assertEquals(0.5, cantidad, 0.0001);
    }
}
