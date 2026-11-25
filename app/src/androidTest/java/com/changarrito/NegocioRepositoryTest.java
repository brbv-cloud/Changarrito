package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.negocio.ControlIntentos;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.negocio.NegocioRepository.Destino;
import com.changarrito.negocio.SesionTurno;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** A dónde manda la app después del login, y claves con bloqueo por intentos. */
@RunWith(AndroidJUnit4.class)
public class NegocioRepositoryTest {

    private static final String UID = "uid-de-prueba";

    private Application app;
    private AppDatabase db;
    private NegocioRepository repo;

    @Before
    public void preparar() {
        app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase.class).allowMainThreadQueries().build();
        AppDatabase.setInstanceParaTest(db);
        SesionTurno.cerrar(app);
        repo = new NegocioRepository(app);
    }

    @After
    public void limpiar() {
        SesionTurno.cerrar(app);
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    private <T> T esperar(java.util.function.Consumer<NegocioRepository.Callback<T>> llamada) throws InterruptedException {
        final Object[] r = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);
        llamada.accept(v -> {
            r[0] = v;
            latch.countDown();
        });
        assertTrue("timeout", latch.await(3, TimeUnit.SECONDS));
        //noinspection unchecked
        return (T) r[0];
    }

    private Destino destino(String uid) throws InterruptedException {
        return esperar(cb -> repo.resolverInicio(uid, "Bruno", cb));
    }

    @Test
    public void primera_vez_pide_giro() throws Exception {
        assertEquals(Destino.ELEGIR_GIRO, destino(UID));
    }

    @Test
    public void giro_con_catalogo_pide_decidir_el_catalogo() throws Exception {
        Boolean tieneCatalogo = esperar(cb -> repo.guardarGiro(GiroNegocio.PAPELERIA, "Papelería Lupita", UID, "Bruno", 40, cb));
        assertTrue(tieneCatalogo);
        assertEquals(Destino.CATALOGO, destino(UID));

        Integer n = esperar(cb -> repo.importarCatalogo(new ArrayList<>(), cb));
        assertEquals(0, (int) n);
        assertEquals(Destino.APP, destino(UID));
    }

    @Test
    public void solo_el_dueno_entra_directo_con_su_turno() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        assertEquals(Destino.APP, destino(UID));
        assertNotNull(SesionTurno.getIdUsuario(app));
        assertTrue(SesionTurno.esDueno(app));
        assertEquals("Bruno", SesionTurno.getNombre(app));
    }

    @Test
    public void con_trabajadores_y_sin_turno_pregunta_quien_atiende() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        this.<Integer>esperar(cb -> repo.crearTrabajador("Ana", "1234", cb));
        SesionTurno.cerrar(app);
        assertEquals(Destino.ELEGIR_TURNO, destino(UID));
    }

    @Test
    public void otra_cuenta_no_entra_a_un_negocio_ajeno() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        assertEquals(Destino.OTRA_CUENTA, destino("otro-uid"));
    }

    @Test
    public void clave_correcta_abre_el_turno_del_trabajador() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        Integer idAna = esperar(cb -> repo.crearTrabajador("Ana", "1234", cb));

        Object[] r = esperar(cb -> repo.verificarClave(idAna, "1234", cb));
        assertEquals(NegocioRepository.ResultadoClave.CORRECTA, r[0]);
        assertEquals(idAna, SesionTurno.getIdUsuario(app));
        assertTrue(!SesionTurno.esDueno(app));
    }

    @Test
    public void cinco_claves_incorrectas_bloquean_el_perfil() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        Integer idAna = esperar(cb -> repo.crearTrabajador("Ana", "1234", cb));

        Object[] r = null;
        for (int i = 0; i < ControlIntentos.MAX_INTENTOS; i++) {
            r = esperar(cb -> repo.verificarClave(idAna, "0000", cb));
        }
        assertEquals(NegocioRepository.ResultadoClave.BLOQUEADO, r[0]);

        // Bloqueado, ni la clave correcta entra.
        Object[] despues = esperar(cb -> repo.verificarClave(idAna, "1234", cb));
        assertEquals(NegocioRepository.ResultadoClave.BLOQUEADO, despues[0]);
    }

    @Test
    public void dar_de_baja_no_borra_al_trabajador() throws Exception {
        this.<Boolean>esperar(cb -> repo.guardarGiro(GiroNegocio.INDEPENDIENTE, "Mi tienda", UID, "Bruno", 30, cb));
        Integer idAna = esperar(cb -> repo.crearTrabajador("Ana", "1234", cb));
        Boolean ok = esperar(cb -> repo.darDeBaja(idAna, cb));
        assertTrue(ok);
        UsuarioEntity ana = db.usuarioDao().getUsuarioByIdSync(idAna);
        assertNotNull(ana);
        assertTrue(!ana.activo);
        assertEquals(1, db.usuarioDao().contarActivosSync());
    }
}
