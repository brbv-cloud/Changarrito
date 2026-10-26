package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Application;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.repository.MermaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MermaRepositoryTest {

    private AppDatabase db;
    private MermaRepository repo;
    private int idCoca, idFrijol;

    @Before
    public void crearBD() {
        Application app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase.class).allowMainThreadQueries().build();
        AppDatabase.setInstanceParaTest(db);
        repo = new MermaRepository(app);
        idCoca = producto("Coca-Cola", UnidadMedida.PZA, 10, 12);
        idFrijol = producto("Frijol", UnidadMedida.KG, 5, 30);
    }

    @After
    public void cerrarBD() {
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    private int producto(String nombre, UnidadMedida u, double existencia, double costo) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = costo * 1.4;
        p.costoCompra = costo;
        p.cantidadDisponible = existencia;
        p.unidadMedida = u;
        return (int) db.productoDao().insert(p);
    }

    private String registrar(int id, double cantidad, MotivoMerma motivo, long ts) {
        String[] error = new String[1];
        double[] quedan = new double[1];
        repo.registrarSync(id, cantidad, motivo, "nota", ts, error, quedan);
        return error[0];
    }

    @Test
    public void merma_descuenta_existencia_y_queda_en_bitacora() {
        assertNull(registrar(idCoca, 3, MotivoMerma.CADUCADO, 1000));
        assertEquals(7, db.productoDao().getProductoByIdSync(idCoca).cantidadDisponible, 0.0001);

        List<BitacoraEntity> b = db.respaldoDao().leerBitacora();
        assertEquals(1, b.size());
        assertEquals(AccionBitacora.REGISTRAR_MERMA, b.get(0).accion);
        assertEquals("10 PZA", b.get(0).valorAnterior);
        assertEquals("7 PZA", b.get(0).valorNuevo);
    }

    @Test
    public void merma_a_granel_acepta_decimales() {
        assertNull(registrar(idFrijol, 0.75, MotivoMerma.DANADO, 1000));
        assertEquals(4.25, db.productoDao().getProductoByIdSync(idFrijol).cantidadDisponible, 0.0001);
    }

    @Test
    public void no_deja_mermar_mas_de_lo_que_hay_ni_decimales_en_piezas() {
        assertNotNull(registrar(idCoca, 11, MotivoMerma.ROBO, 1000));
        assertNotNull(registrar(idCoca, 1.5, MotivoMerma.ROBO, 1000));
        assertNotNull(registrar(idCoca, 0, MotivoMerma.ROBO, 1000));
        assertEquals(10, db.productoDao().getProductoByIdSync(idCoca).cantidadDisponible, 0.0001);
        assertEquals(0, db.respaldoDao().leerMermas().size());
    }

    @Test
    public void reporte_por_motivo_suma_el_costo_perdido() {
        registrar(idCoca, 2, MotivoMerma.CADUCADO, 1000);   // 2 × 12 = 24
        registrar(idCoca, 1, MotivoMerma.CADUCADO, 2000);   // 12
        registrar(idFrijol, 0.5, MotivoMerma.DANADO, 3000); // 15

        List<MermaPorMotivo> porMotivo = db.mermaDao().getPorMotivoSync(0, 5000);
        assertEquals(2, porMotivo.size());
        assertEquals("CADUCADO", porMotivo.get(0).motivo);
        assertEquals(36, porMotivo.get(0).costo, 0.001);
        assertEquals(2, porMotivo.get(0).registros);

        List<MermaConUsuario> lista = db.mermaDao().getEnRangoSync(0, 5000);
        assertEquals(3, lista.size());
        assertEquals("Frijol", lista.get(0).merma.nombreProducto); // la mas reciente primero
    }
}
