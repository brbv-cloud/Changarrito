package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorUsuario;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Consultas del reporte de Ganancias (VentaDAO y CierreCajaDAO) contra Room en memoria. */
@RunWith(AndroidJUnit4.class)
public class GananciasDAOTest {

    private static final double D = 0.001;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private long ahora;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        ahora = System.currentTimeMillis();
    }

    @After
    public void cerrarBD() {
        db.close();
    }

    // ---------- helpers ----------

    private int producto(String nombre, UnidadMedida unidad) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = 10;
        p.cantidadDisponible = 100;
        p.unidadMedida = unidad;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "";
        p.barcode = "";
        p.timestampCreacionMs = ahora;
        p.timestampActualizacionMs = ahora;
        return (int) db.productoDao().insert(p);
    }

    private int usuario(String nombre, Rol rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.nombre = nombre;
        u.rol = rol;
        u.claveHash = "x";
        u.claveSal = "y";
        u.timestampCreacionMs = ahora;
        return (int) db.usuarioDao().insert(u);
    }

    private void venta(int idProducto, double cantidad, double precio, double costo, Integer idUsuario, long ts) {
        VentaEntity v = new VentaEntity();
        v.idProducto = idProducto;
        v.cantidadVendida = cantidad;
        v.unidadMedida = UnidadMedida.PZA;
        v.precioUnitarioVenta = precio;
        v.costoUnitarioVenta = costo;
        v.total = cantidad * precio;
        v.timestampVentaMs = ts;
        v.idUsuario = idUsuario;
        db.ventaDao().insert(v);
    }

    private void cierre(Integer idUsuario, double diferencia, long ts) {
        CierreCajaEntity c = new CierreCajaEntity();
        c.fechaMs = ts;
        c.efectivoSistema = 100;
        c.efectivoFisicoContado = 100 + diferencia;
        c.diferencia = diferencia;
        c.notas = "";
        c.timestampCreacionMs = ts;
        c.idUsuario = idUsuario;
        db.cierreCajaDao().insert(c);
    }

    private <T> T valor(LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T v) {
                data[0] = v;
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
    public void resumen_separaVentasSinCosto() throws Exception {
        int coca = producto("Coca", UnidadMedida.PZA);
        int dulce = producto("Dulce", UnidadMedida.PZA);
        venta(coca, 2, 37.5, 25, null, ahora);   // vende 75, cuesta 50
        venta(dulce, 1, 10, 0, null, ahora);     // sin costo registrado

        ResumenGanancia r = valor(db.ventaDao().getResumenGanancia(ahora - 1000, ahora + 1000));
        assertEquals(85, r.ventas, D);
        assertEquals(75, r.ventasConCosto, D);
        assertEquals(50, r.costo, D);
        assertEquals(1, r.ventasSinCosto);
        assertEquals(10, r.importeSinCosto, D);
        assertEquals(2, r.transacciones);
    }

    @Test
    public void resumen_sinVentas_devuelveCeros() throws Exception {
        ResumenGanancia r = valor(db.ventaDao().getResumenGanancia(ahora - 1000, ahora + 1000));
        assertEquals(0, r.ventas, D);
        assertEquals(0, r.transacciones);
    }

    @Test
    public void resumen_respetaElRango() throws Exception {
        int id = producto("A", UnidadMedida.PZA);
        venta(id, 1, 20, 10, null, ahora);
        venta(id, 1, 20, 10, null, ahora - 3L * 24 * 60 * 60 * 1000);
        ResumenGanancia r = valor(db.ventaDao().getResumenGanancia(ahora - 1000, ahora + 1000));
        assertEquals(20, r.ventas, D);
    }

    @Test
    public void porProducto_ordenaPorGananciaYCalculaGranel() throws Exception {
        int poca = producto("Poca", UnidadMedida.PZA);
        int mucha = producto("Mucha", UnidadMedida.PZA);
        int frijol = producto("Frijol", UnidadMedida.KG);
        venta(poca, 1, 11, 10, null, ahora);        // gana 1
        venta(mucha, 1, 30, 10, null, ahora);       // gana 20
        venta(frijol, 0.75, 40, 30, null, ahora);   // vende 30, cuesta 22.5, gana 7.5

        List<GananciaProducto> lista = valor(db.ventaDao().getGananciaPorProducto(ahora - 1000, ahora + 1000));
        assertEquals(3, lista.size());
        assertEquals("Mucha", lista.get(0).nombre);
        assertEquals("Frijol", lista.get(1).nombre);
        assertEquals(22.5, lista.get(1).costo, D);
        assertEquals(7.5, lista.get(1).ganancia(), D);
        assertEquals("Poca", lista.get(2).nombre);
    }

    @Test
    public void porUsuario_agrupaYSinTurnoQuedaNulo() throws Exception {
        int ana = usuario("Ana", Rol.TRABAJADOR);
        int p = producto("A", UnidadMedida.PZA);
        venta(p, 1, 100, 50, ana, ahora);
        venta(p, 1, 50, 25, ana, ahora);
        venta(p, 1, 10, 5, null, ahora);

        List<VentasPorUsuario> lista = valor(db.ventaDao().getVentasPorUsuario(ahora - 1000, ahora + 1000));
        assertEquals(2, lista.size());
        assertEquals("Ana", lista.get(0).nombre);
        assertEquals(2, lista.get(0).transacciones);
        assertEquals(150, lista.get(0).ventas, D);
        assertNull(lista.get(1).idUsuario);
    }

    @Test
    public void cierresPorUsuario_separaFaltanteYSobrante() throws Exception {
        int ana = usuario("Ana", Rol.TRABAJADOR);
        cierre(ana, -15, ahora);
        cierre(ana, 5, ahora);
        cierre(ana, 0, ahora);

        List<CierresPorUsuario> lista = valor(db.cierreCajaDao().getCierresPorUsuario(ahora - 1000, ahora + 1000));
        assertEquals(1, lista.size());
        assertEquals(3, lista.get(0).cierres);
        assertEquals(-15, lista.get(0).faltante, D);
        assertEquals(5, lista.get(0).sobrante, D);
    }
}
