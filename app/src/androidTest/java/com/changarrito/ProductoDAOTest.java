package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ProductoDAOTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ProductoDAO dao;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        // BD en memoria: se destruye al terminar, no toca la BD real del usuario
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.productoDao();
    }

    @After
    public void cerrarBD() {
        db.close();
    }

    // ---------- helpers ----------

    private ProductoEntity nuevoProducto(String nombre, double cantidad, UnidadMedida unidad) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = 25.50;
        p.cantidadDisponible = cantidad;
        p.unidadMedida = unidad;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "3312345678";
        p.barcode = "";
        p.timestampCreacionMs = System.currentTimeMillis();
        p.timestampActualizacionMs = System.currentTimeMillis();
        return p;
    }

    /** Extrae el valor de un LiveData esperando a que emita. */
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
    public void insertar_y_leer_producto() throws Exception {
        long id = dao.insert(nuevoProducto("Arroz", 5.0, UnidadMedida.KG));
        assertTrue(id > 0);

        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        assertEquals(1, todos.size());
        assertEquals("Arroz", todos.get(0).nombre);
    }

    @Test
    public void insertar_conserva_cantidad_decimal() throws Exception {
        dao.insert(nuevoProducto("Frijol", 0.750, UnidadMedida.KG));

        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        // El punto clave del granel: no se trunca a entero
        assertEquals(0.750, todos.get(0).cantidadDisponible, 0.0001);
    }

    @Test
    public void insertar_conserva_unidad_de_medida() throws Exception {
        dao.insert(nuevoProducto("Aceite", 3.0, UnidadMedida.LT));

        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        assertEquals(UnidadMedida.LT, todos.get(0).unidadMedida);
    }

    @Test
    public void insertAll_carga_catalogo_completo() throws Exception {
        List<ProductoEntity> catalogo = new ArrayList<>(Arrays.asList(
                nuevoProducto("Arroz", 1, UnidadMedida.KG),
                nuevoProducto("Leche 1L", 6, UnidadMedida.PZA),
                nuevoProducto("Canela", 500, UnidadMedida.GR)
        ));
        dao.insertAll(catalogo);

        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        assertEquals(3, todos.size());
    }

    @Test
    public void actualizar_producto() throws Exception {
        long id = dao.insert(nuevoProducto("Azucar", 10.0, UnidadMedida.KG));

        ProductoEntity p = getValor(dao.getAllProductos()).get(0);
        p.precio = 99.99;
        p.cantidadDisponible = 2.5;
        int filas = dao.update(p);

        assertEquals(1, filas);

        ProductoEntity actualizado = getValor(dao.getProductoById((int) id));
        assertEquals(99.99, actualizado.precio, 0.001);
        assertEquals(2.5, actualizado.cantidadDisponible, 0.0001);
    }

    @Test
    public void eliminar_producto() throws Exception {
        dao.insert(nuevoProducto("Sal", 4.0, UnidadMedida.KG));

        ProductoEntity p = getValor(dao.getAllProductos()).get(0);
        int filas = dao.delete(p);
        assertEquals(1, filas);

        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        assertTrue(todos.isEmpty());
    }

    @Test
    public void buscar_por_nombre_parcial() throws Exception {
        dao.insert(nuevoProducto("Leche entera 1L", 6, UnidadMedida.PZA));
        dao.insert(nuevoProducto("Leche deslactosada 1L", 4, UnidadMedida.PZA));
        dao.insert(nuevoProducto("Arroz", 3, UnidadMedida.KG));

        List<ProductoEntity> resultado = getValor(dao.searchProductoByName("Leche"));
        assertEquals(2, resultado.size());
    }

    @Test
    public void buscar_por_barcode_existente() throws Exception {
        ProductoEntity p = nuevoProducto("Coca Cola 600ml", 12, UnidadMedida.PZA);
        p.barcode = "7501234567890";
        dao.insert(p);

        ProductoEntity encontrado = getValor(dao.getProductoByBarcode("7501234567890"));
        assertNotNull(encontrado);
        assertEquals("Coca Cola 600ml", encontrado.nombre);
    }

    @Test
    public void buscar_por_barcode_inexistente_devuelve_null() throws Exception {
        dao.insert(nuevoProducto("Pan", 5, UnidadMedida.PZA));

        ProductoEntity encontrado = getValor(dao.getProductoByBarcode("0000000000000"));
        assertNull(encontrado);
    }

    @Test
    public void productos_proximos_a_vencer() throws Exception {
        long ahora = System.currentTimeMillis();
        long unDia = 24L * 60 * 60 * 1000;

        ProductoEntity pronto = nuevoProducto("Yogurt", 5, UnidadMedida.PZA);
        pronto.fechaCaducidadMs = ahora + (3 * unDia);   // vence en 3 días
        dao.insert(pronto);

        ProductoEntity lejano = nuevoProducto("Atun", 10, UnidadMedida.PZA);
        lejano.fechaCaducidadMs = ahora + (60 * unDia);  // vence en 60 días
        dao.insert(lejano);

        long limite = ahora + (7 * unDia);
        List<ProductoEntity> porVencer = getValor(dao.getProductosProximosAVencer(ahora, limite));

        assertEquals(1, porVencer.size());
        assertEquals("Yogurt", porVencer.get(0).nombre);
    }

    @Test
    public void bd_vacia_devuelve_lista_vacia() throws Exception {
        List<ProductoEntity> todos = getValor(dao.getAllProductos());
        assertTrue(todos.isEmpty());
    }
}