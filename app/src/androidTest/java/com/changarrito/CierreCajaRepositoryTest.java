package com.changarrito;

import static org.junit.Assert.assertEquals;
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
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.repository.CierreCajaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Prueba CierreCajaRepository.registrarCierre(...) contra una BD Room EN MEMORIA,
 * mismo patron que VentaRepositoryTest/AlertaRepositoryTest.
 */
@RunWith(AndroidJUnit4.class)
public class CierreCajaRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ProductoDAO productoDao;
    private VentaDAO ventaDao;
    private CierreCajaRepository cierreCajaRepository;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceParaTest(db);

        productoDao = db.productoDao();
        ventaDao = db.ventaDao();
        cierreCajaRepository = new CierreCajaRepository((Application) context);
    }

    @After
    public void cerrarBD() {
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    // ---------- helpers ----------

    private void registrarVentaDeHoy(double total) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = "Producto";
        p.precio = total;
        p.cantidadDisponible = 100;
        p.unidadMedida = UnidadMedida.PZA;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "";
        p.barcode = "";
        p.timestampCreacionMs = System.currentTimeMillis();
        p.timestampActualizacionMs = System.currentTimeMillis();
        int id = (int) productoDao.insert(p);

        VentaEntity v = new VentaEntity();
        v.idProducto = id;
        v.cantidadVendida = 1;
        v.unidadMedida = UnidadMedida.PZA;
        v.precioUnitarioVenta = total;
        v.total = total;
        v.timestampVentaMs = System.currentTimeMillis();
        v.usuarioRegistro = "local";
        ventaDao.insert(v);
    }

    private CierreCajaEntity hacerCierreYEsperar(double efectivoContado) throws InterruptedException {
        final CierreCajaEntity[] resultado = new CierreCajaEntity[1];
        CountDownLatch latch = new CountDownLatch(1);
        cierreCajaRepository.registrarCierre(efectivoContado, "", c -> {
            resultado[0] = c;
            latch.countDown();
        });
        assertTrue("timeout esperando registrarCierre", latch.await(2, TimeUnit.SECONDS));
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
    public void cierre_exacto_diferencia_cero() throws Exception {
        registrarVentaDeHoy(150.0);

        CierreCajaEntity cierre = hacerCierreYEsperar(150.0);

        assertEquals(150.0, cierre.efectivoSistema, 0.001);
        assertEquals(0.0, cierre.diferencia, 0.001);
    }

    @Test
    public void cierre_con_sobrante_diferencia_positiva() throws Exception {
        registrarVentaDeHoy(100.0);

        CierreCajaEntity cierre = hacerCierreYEsperar(120.0);

        assertEquals(20.0, cierre.diferencia, 0.001);
    }

    @Test
    public void cierre_con_faltante_diferencia_negativa() throws Exception {
        registrarVentaDeHoy(100.0);

        CierreCajaEntity cierre = hacerCierreYEsperar(80.0);

        assertEquals(-20.0, cierre.diferencia, 0.001);
    }

    @Test
    public void cierre_sin_ventas_del_dia_no_truena() throws Exception {
        CierreCajaEntity cierre = hacerCierreYEsperar(0.0);

        assertEquals(0.0, cierre.efectivoSistema, 0.001);
        assertEquals(0.0, cierre.diferencia, 0.001);
    }

    @Test
    public void cierre_sin_ventas_pero_con_efectivo_contado_marca_sobrante() throws Exception {
        CierreCajaEntity cierre = hacerCierreYEsperar(50.0);

        assertEquals(50.0, cierre.diferencia, 0.001);
    }

    @Test
    public void cierre_se_guarda_en_el_historial() throws Exception {
        registrarVentaDeHoy(75.0);
        hacerCierreYEsperar(75.0);

        List<CierreCajaEntity> historial = getValor(cierreCajaRepository.getHistorial());
        assertEquals(1, historial.size());
    }

    @Test
    public void dos_cierres_seguidos_ambos_quedan_en_el_historial() throws Exception {
        registrarVentaDeHoy(50.0);
        hacerCierreYEsperar(50.0);
        hacerCierreYEsperar(55.0);

        List<CierreCajaEntity> historial = getValor(cierreCajaRepository.getHistorial());
        assertEquals(2, historial.size());
    }

    /**
     * El "dia" se restablece al cerrar: un segundo cierre el mismo dia solo debe
     * contar lo vendido DESPUES del primer cierre, no repetir la venta ya conciliada.
     */
    @Test
    public void segundo_cierre_solo_cuenta_ventas_posteriores_al_primero() throws Exception {
        registrarVentaDeHoy(100.0);
        hacerCierreYEsperar(100.0);

        registrarVentaDeHoy(30.0);
        CierreCajaEntity segundoCierre = hacerCierreYEsperar(30.0);

        assertEquals(30.0, segundoCierre.efectivoSistema, 0.001);
        assertEquals(0.0, segundoCierre.diferencia, 0.001);
    }

    /** Si no hubo ventas nuevas desde el ultimo cierre, el efectivo pendiente es 0, no el del dia completo. */
    @Test
    public void segundo_cierre_sin_ventas_nuevas_efectivo_pendiente_es_cero() throws Exception {
        registrarVentaDeHoy(100.0);
        hacerCierreYEsperar(100.0);

        CierreCajaEntity segundoCierre = hacerCierreYEsperar(0.0);

        assertEquals(0.0, segundoCierre.efectivoSistema, 0.001);
        assertEquals(0.0, segundoCierre.diferencia, 0.001);
    }
}
