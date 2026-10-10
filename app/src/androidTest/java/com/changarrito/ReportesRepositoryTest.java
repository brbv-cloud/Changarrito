package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.IngresoDia;
import com.changarrito.database.dao.ProductoConTotal;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.VentaEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Prueba las queries nuevas de reportes en VentaDAO (Sprint 4) contra una BD Room
 * en memoria: agrupacion de ingresos por dia, producto mas vendido en un rango,
 * y conteo de transacciones.
 */
@RunWith(AndroidJUnit4.class)
public class ReportesRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ProductoDAO productoDao;
    private VentaDAO ventaDao;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        productoDao = db.productoDao();
        ventaDao = db.ventaDao();
    }

    @After
    public void cerrarBD() {
        db.close();
    }

    // ---------- helpers ----------

    private int nuevoProducto(String nombre) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = 10;
        p.cantidadDisponible = 100;
        p.unidadMedida = UnidadMedida.PZA;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "";
        p.barcode = "";
        p.timestampCreacionMs = System.currentTimeMillis();
        p.timestampActualizacionMs = System.currentTimeMillis();
        return (int) productoDao.insert(p);
    }

    private void registrarVenta(int idProducto, double total, long timestampMs) {
        VentaEntity v = new VentaEntity();
        v.idProducto = idProducto;
        v.cantidadVendida = 1;
        v.unidadMedida = UnidadMedida.PZA;
        v.precioUnitarioVenta = total;
        v.total = total;
        v.timestampVentaMs = timestampMs;
        v.usuarioRegistro = "local";
        ventaDao.insert(v);
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
    public void getTotalVentasPorDiaSync_suma_solo_el_rango_pedido() {
        int id = nuevoProducto("A");
        long ahora = System.currentTimeMillis();
        long unDia = 24L * 60 * 60 * 1000;

        registrarVenta(id, 100, ahora);
        registrarVenta(id, 50, ahora - (2 * unDia)); // fuera del rango de "hoy"

        double totalHoy = ventaDao.getTotalVentasPorDiaSync(ahora - 1000, ahora + 1000);
        assertEquals(100.0, totalHoy, 0.001);
    }

    @Test
    public void getCountVentasEnRango_cuenta_transacciones_no_dinero() throws Exception {
        int id = nuevoProducto("B");
        long ahora = System.currentTimeMillis();
        registrarVenta(id, 10, ahora);
        registrarVenta(id, 20, ahora);
        registrarVenta(id, 30, ahora);

        Integer conteo = getValor(ventaDao.getCountVentasEnRango(ahora - 1000, ahora + 1000));
        assertEquals(Integer.valueOf(3), conteo);
    }

    @Test
    public void getProductoMasVendidoEnRango_devuelve_el_de_mayor_total() throws Exception {
        int idBarato = nuevoProducto("Barato");
        int idCaro = nuevoProducto("Caro");
        long ahora = System.currentTimeMillis();

        registrarVenta(idBarato, 10, ahora);
        registrarVenta(idCaro, 500, ahora);

        ProductoConTotal top = getValor(ventaDao.getProductoMasVendidoEnRango(ahora - 1000, ahora + 1000));
        assertNotNull(top);
        assertEquals("Caro", top.nombre);
    }

    @Test
    public void getProductoMasVendidoEnRango_sin_ventas_devuelve_null() throws Exception {
        ProductoConTotal top = getValor(ventaDao.getProductoMasVendidoEnRango(0, System.currentTimeMillis()));
        assertNull(top);
    }

    @Test
    public void getIngresosPorDiaEnRango_agrupa_por_dia() throws Exception {
        int id = nuevoProducto("C");
        long ahora = System.currentTimeMillis();
        long unDia = 24L * 60 * 60 * 1000;

        registrarVenta(id, 100, ahora);
        registrarVenta(id, 50, ahora);
        registrarVenta(id, 30, ahora - unDia);

        List<IngresoDia> ingresos = getValor(ventaDao.getIngresosPorDiaEnRango(ahora - (2 * unDia), ahora + 1000));
        assertEquals(2, ingresos.size());
    }

    @Test
    public void getIngresosPorDiaEnRango_sin_ventas_devuelve_lista_vacia() throws Exception {
        List<IngresoDia> ingresos = getValor(ventaDao.getIngresosPorDiaEnRango(0, System.currentTimeMillis()));
        assertEquals(0, ingresos.size());
    }
}
