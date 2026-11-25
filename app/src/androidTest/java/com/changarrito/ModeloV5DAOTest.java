package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ModoCaducidad;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.utils.ClaveUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Tablas nuevas del modelo v5: Usuario, Bitacora y ConfiguracionNegocio. */
@RunWith(AndroidJUnit4.class)
public class ModeloV5DAOTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void cerrarBD() {
        db.close();
    }

    // ---------- Usuario ----------

    private UsuarioEntity nuevoUsuario(String nombre, Rol rol, String clave) {
        UsuarioEntity u = new UsuarioEntity();
        u.nombre = nombre;
        u.rol = rol;
        u.claveSal = ClaveUtil.generarSal();
        u.claveHash = ClaveUtil.hashear(clave, u.claveSal);
        u.timestampCreacionMs = System.currentTimeMillis();
        return u;
    }

    @Test
    public void usuario_guarda_hash_y_no_la_clave() {
        int id = (int) db.usuarioDao().insert(nuevoUsuario("Ana", Rol.TRABAJADOR, "1234"));

        UsuarioEntity leido = db.usuarioDao().getUsuarioByIdSync(id);
        assertEquals(Rol.TRABAJADOR, leido.rol);
        assertNotEquals("1234", leido.claveHash);
        assertTrue(ClaveUtil.verificar("1234", leido.claveSal, leido.claveHash));
        assertFalse(ClaveUtil.verificar("4321", leido.claveSal, leido.claveHash));
        assertTrue(leido.activo);
        assertEquals(0, leido.intentosFallidos);
    }

    @Test
    public void lista_de_activos_excluye_dados_de_baja() throws Exception {
        db.usuarioDao().insert(nuevoUsuario("Bruno", Rol.DUENO, "0000"));
        int idAna = (int) db.usuarioDao().insert(nuevoUsuario("Ana", Rol.TRABAJADOR, "1111"));
        UsuarioEntity ana = db.usuarioDao().getUsuarioByIdSync(idAna);
        ana.activo = false;
        db.usuarioDao().update(ana);

        List<UsuarioEntity> activos = getValor(db.usuarioDao().getUsuariosActivos());
        assertEquals(1, activos.size());
        assertEquals(Rol.DUENO, activos.get(0).rol);
        assertEquals(1, db.usuarioDao().contarActivosSync());
        assertEquals("Bruno", db.usuarioDao().getDuenoSync().nombre);
    }

    @Test
    public void venta_registra_quien_vendio() throws Exception {
        int idUsuario = (int) db.usuarioDao().insert(nuevoUsuario("Ana", Rol.TRABAJADOR, "1111"));
        ProductoEntity p = new ProductoEntity();
        p.nombre = "Lápiz";
        p.precio = 8;
        p.cantidadDisponible = 10;
        p.unidadMedida = UnidadMedida.PZA;
        int idProducto = (int) db.productoDao().insert(p);

        VentaEntity v = new VentaEntity();
        v.idProducto = idProducto;
        v.cantidadVendida = 1;
        v.unidadMedida = UnidadMedida.PZA;
        v.precioUnitarioVenta = 8;
        v.total = 8;
        v.timestampVentaMs = System.currentTimeMillis();
        v.idUsuario = idUsuario;
        db.ventaDao().insert(v);

        assertEquals(Integer.valueOf(idUsuario), getValor(db.ventaDao().getAllVentas()).get(0).idUsuario);
    }

    // ---------- Bitacora ----------

    @Test
    public void bitacora_devuelve_lo_mas_reciente_primero() throws Exception {
        BitacoraEntity viejo = entrada(1_000L, "precio", "10.00", "12.00");
        BitacoraEntity nuevo = entrada(2_000L, "precio", "12.00", "9.00");
        db.bitacoraDao().insert(viejo);
        db.bitacoraDao().insert(nuevo);

        List<BitacoraEntity> recientes = getValor(db.bitacoraDao().getRecientes(10));
        assertEquals(2, recientes.size());
        assertEquals("9.00", recientes.get(0).valorNuevo);
        assertEquals(AccionBitacora.EDITAR_PRODUCTO, recientes.get(0).accion);
    }

    private BitacoraEntity entrada(long ts, String campo, String antes, String despues) {
        BitacoraEntity b = new BitacoraEntity();
        b.timestampMs = ts;
        b.nombreProducto = "Cuaderno";
        b.accion = AccionBitacora.EDITAR_PRODUCTO;
        b.campo = campo;
        b.valorAnterior = antes;
        b.valorNuevo = despues;
        return b;
    }

    // ---------- ConfiguracionNegocio ----------

    @Test
    public void sin_configuracion_devuelve_null() {
        assertNull(db.configuracionNegocioDao().getConfiguracionSync());
    }

    @Test
    public void configuracion_por_giro_se_guarda_y_se_reemplaza() {
        db.configuracionNegocioDao().guardar(
                ConfiguracionNegocioEntity.paraGiro(GiroNegocio.FERRETERIA, "uid123", 30));

        ConfiguracionNegocioEntity c = db.configuracionNegocioDao().getConfiguracionSync();
        assertEquals(GiroNegocio.FERRETERIA, c.giro);
        assertEquals(ModoCaducidad.OCULTA, c.modoCaducidad);
        assertTrue(c.getUnidades().contains(UnidadMedida.M));
        assertFalse(c.getUnidades().contains(UnidadMedida.GR));

        // Guardar de nuevo reemplaza la única fila, no crea otra.
        db.configuracionNegocioDao().guardar(
                ConfiguracionNegocioEntity.paraGiro(GiroNegocio.FARMACIA, "uid123", 25));
        c = db.configuracionNegocioDao().getConfiguracionSync();
        assertEquals(GiroNegocio.FARMACIA, c.giro);
        assertEquals(ModoCaducidad.OBLIGATORIA, c.modoCaducidad);
        assertEquals(25, c.margenPorDefecto, 0.001);
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
}
