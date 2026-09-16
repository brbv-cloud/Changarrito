package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.database.dao.AlertaDAO;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.entity.AlertaEntity;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.repository.AlertaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Prueba AlertaRepository.verificarAlertasSync() contra una BD Room EN MEMORIA,
 * igual patron que VentaRepositoryTest: AppDatabase.setInstanceParaTest(...) para
 * no tocar la BD real del dispositivo.
 */
@RunWith(AndroidJUnit4.class)
public class AlertaRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ProductoDAO productoDao;
    private AlertaDAO alertaDao;
    private AlertaRepository alertaRepository;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceParaTest(db);

        productoDao = db.productoDao();
        alertaDao = db.alertaDao();
        alertaRepository = new AlertaRepository((Application) context);
    }

    @After
    public void cerrarBD() {
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    // ---------- helpers ----------

    private ProductoEntity nuevoProducto(String nombre, double cantidad, double umbral, UnidadMedida unidad) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = 25.0;
        p.cantidadDisponible = cantidad;
        p.unidadMedida = unidad;
        p.umbralStockBajo = umbral;
        p.telefonoProveedor = "3312345678";
        p.barcode = "";
        p.timestampCreacionMs = System.currentTimeMillis();
        p.timestampActualizacionMs = System.currentTimeMillis();
        return p;
    }

    /** Corre verificarAlertasSync() de forma sincrona en el hilo de prueba (sin pasar por el ExecutorService del repository). */
    private void verificarSync() {
        alertaRepository.verificarAlertasSync();
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
    public void stock_bajo_del_umbral_crea_alerta() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Arroz", 1.0, 2.0, UnidadMedida.KG));

        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertEquals(1, alertas.size());
        assertEquals(TipoAlerta.STOCK_BAJO, alertas.get(0).tipoAlerta);
        assertEquals(id, alertas.get(0).idProducto);
    }

    @Test
    public void stock_por_encima_del_umbral_no_crea_alerta() throws Exception {
        productoDao.insert(nuevoProducto("Aceite", 10.0, 2.0, UnidadMedida.LT));

        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertTrue(alertas.isEmpty());
    }

    @Test
    public void producto_proximo_a_vencer_crea_alerta() throws Exception {
        long ahora = System.currentTimeMillis();
        long unDia = 24L * 60 * 60 * 1000;

        ProductoEntity p = nuevoProducto("Yogurt", 10.0, 2.0, UnidadMedida.PZA);
        p.fechaCaducidadMs = ahora + (3 * unDia);
        int id = (int) productoDao.insert(p);

        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertEquals(1, alertas.size());
        assertEquals(TipoAlerta.PROXIMO_VENCIMIENTO, alertas.get(0).tipoAlerta);
        assertEquals(id, alertas.get(0).idProducto);
    }

    @Test
    public void producto_lejos_de_vencer_no_crea_alerta() throws Exception {
        long ahora = System.currentTimeMillis();
        long unDia = 24L * 60 * 60 * 1000;

        ProductoEntity p = nuevoProducto("Atun", 10.0, 2.0, UnidadMedida.PZA);
        p.fechaCaducidadMs = ahora + (60 * unDia);
        productoDao.insert(p);

        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertTrue(alertas.isEmpty());
    }

    @Test
    public void producto_sin_fecha_de_caducidad_no_crea_alerta_de_vencimiento() throws Exception {
        ProductoEntity p = nuevoProducto("Escoba", 10.0, 2.0, UnidadMedida.PZA);
        p.fechaCaducidadMs = 0; // sin fecha capturada

        productoDao.insert(p);

        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertTrue(alertas.isEmpty());
    }

    @Test
    public void verificar_dos_veces_no_duplica_alerta_de_stock_bajo() throws Exception {
        productoDao.insert(nuevoProducto("Sal", 1.0, 3.0, UnidadMedida.KG));

        verificarSync();
        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertEquals(1, alertas.size());
    }

    @Test
    public void marcar_como_resuelta_cambia_estado() throws Exception {
        productoDao.insert(nuevoProducto("Pan", 1.0, 3.0, UnidadMedida.PZA));
        verificarSync();

        List<AlertaEntity> activas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertEquals(1, activas.size());
        int idAlerta = activas.get(0).id;

        alertaDao.actualizarEstado(idAlerta, EstadoAlerta.RESUELTA, System.currentTimeMillis());

        List<AlertaEntity> activasDespues = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertTrue(activasDespues.isEmpty());

        List<AlertaEntity> resueltas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.RESUELTA));
        assertEquals(1, resueltas.size());
    }

    @Test
    public void resolver_y_volver_a_verificar_crea_nueva_alerta_si_sigue_bajo() throws Exception {
        int id = (int) productoDao.insert(nuevoProducto("Huevo", 1.0, 3.0, UnidadMedida.PZA));
        verificarSync();

        List<AlertaEntity> activas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        alertaDao.actualizarEstado(activas.get(0).id, EstadoAlerta.RESUELTA, System.currentTimeMillis());

        verificarSync();

        List<AlertaEntity> nuevasActivas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertEquals(1, nuevasActivas.size());
        assertEquals(id, nuevasActivas.get(0).idProducto);
    }

    @Test
    public void alertas_con_producto_trae_datos_del_producto_para_mostrar_en_ui() throws Exception {
        productoDao.insert(nuevoProducto("Frijol", 0.5, 2.0, UnidadMedida.KG));

        verificarSync();

        List<AlertaConProducto> alertas = getValor(alertaDao.getAlertasConProducto(EstadoAlerta.ACTIVA));
        assertEquals(1, alertas.size());
        assertEquals("Frijol", alertas.get(0).nombreProducto);
        assertEquals(UnidadMedida.KG, alertas.get(0).unidadMedida);
        assertEquals("3312345678", alertas.get(0).telefonoProveedor);
    }

    @Test
    public void bd_vacia_no_genera_alertas() throws Exception {
        verificarSync();

        List<AlertaEntity> alertas = getValor(alertaDao.getAlertasPorEstado(EstadoAlerta.ACTIVA));
        assertTrue(alertas.isEmpty());
    }

    @Test
    public void getAlertaExistenteSync_devuelve_null_cuando_no_hay_alerta() {
        AlertaEntity existente = alertaDao.getAlertaExistenteSync(999, TipoAlerta.STOCK_BAJO, EstadoAlerta.ACTIVA);
        assertNull(existente);
    }
}
