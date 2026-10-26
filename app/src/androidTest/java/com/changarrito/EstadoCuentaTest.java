package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierreConUsuario;
import com.changarrito.database.dao.GananciaDia;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.pdf.GeneradorEstadoCuenta;
import com.changarrito.reportes.DatosEstadoCuenta;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.repository.EstadoCuentaRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/** Consultas del estado de cuenta y generacion real del PDF con datos de ejemplo. */
@RunWith(AndroidJUnit4.class)
public class EstadoCuentaTest {

    private static final long DIA = 24L * 60 * 60 * 1000;

    private AppDatabase db;
    private long ahora;
    private int idAna;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        AppDatabase.setInstanceParaTest(db);
        ahora = System.currentTimeMillis();

        ConfiguracionNegocioEntity c = ConfiguracionNegocioEntity.paraGiro(GiroNegocio.ABARROTES, "uid", 30);
        c.nombreNegocio = "Abarrotes Lupita & Hijos";
        db.configuracionNegocioDao().guardar(c);

        UsuarioEntity ana = new UsuarioEntity();
        ana.nombre = "Ana";
        ana.rol = Rol.TRABAJADOR;
        idAna = (int) db.usuarioDao().insert(ana);

        int coca = producto("Coca-Cola 600 ml", UnidadMedida.PZA, 17, 12);
        int frijol = producto("Frijol a granel con un nombre muy largo para probar el recorte", UnidadMedida.KG, 40, 30);
        int dulce = producto("Dulce sin costo", UnidadMedida.PZA, 5, 0);
        for (int i = 0; i < 6; i++) {
            venta(coca, 3, 17, 12, ahora - i * DIA);
            venta(frijol, 0.75, 40, 30, ahora - i * DIA);
        }
        venta(dulce, 2, 5, 0, ahora);
        venta(coca, 1, 10, 12, ahora - DIA);   // vendida bajo costo

        CierreCajaEntity cierre = new CierreCajaEntity();
        cierre.fechaMs = ahora;
        cierre.efectivoSistema = 100;
        cierre.efectivoFisicoContado = 95;
        cierre.diferencia = -5;
        cierre.notas = "cambio mal dado";
        cierre.timestampCreacionMs = ahora;
        cierre.idUsuario = idAna;
        db.cierreCajaDao().insert(cierre);
    }

    @After
    public void cerrarBD() {
        AppDatabase.setInstanceParaTest(null);
        db.close();
    }

    private int producto(String nombre, UnidadMedida u, double precio, double costo) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = precio;
        p.costoCompra = costo;
        p.cantidadDisponible = 100;
        p.unidadMedida = u;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "";
        p.barcode = "";
        return (int) db.productoDao().insert(p);
    }

    private void venta(int idProducto, double cantidad, double precio, double costo, long ts) {
        VentaEntity v = new VentaEntity();
        v.idProducto = idProducto;
        v.cantidadVendida = cantidad;
        v.unidadMedida = UnidadMedida.PZA;
        v.precioUnitarioVenta = precio;
        v.costoUnitarioVenta = costo;
        v.total = cantidad * precio;
        v.timestampVentaMs = ts;
        v.idUsuario = idAna;
        db.ventaDao().insert(v);
    }

    @Test
    public void ganancia_por_dia_agrupa_por_fecha() {
        long[] r = RangoReporte.SIETE_DIAS.calcular(ahora);
        List<GananciaDia> dias = db.ventaDao().getGananciaPorDiaSync(r[0], r[1]);
        assertEquals(6, dias.size());
        GananciaDia hoy = dias.get(dias.size() - 1);
        // 3 cocas (51) + 0.75 kg frijol (30) + 2 dulces sin costo (10)
        assertEquals(91, hoy.ventas, 0.001);
        assertEquals(81, hoy.ventasConCosto, 0.001);
        assertEquals(36 + 22.5, hoy.costo, 0.001);
    }

    @Test
    public void cierres_en_rango_traen_quien_los_hizo() {
        List<CierreConUsuario> cierres = db.cierreCajaDao().getCierresEnRangoSync(ahora - 1000, ahora + 1000);
        assertEquals(1, cierres.size());
        assertEquals("Ana", cierres.get(0).nombreUsuario);
        assertEquals(-5, cierres.get(0).cierre.diferencia, 0.001);
    }

    @Test
    public void genera_un_pdf_valido_con_7_y_30_dias() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        for (RangoReporte rango : new RangoReporte[]{RangoReporte.HOY, RangoReporte.SIETE_DIAS, RangoReporte.TREINTA_DIAS}) {
            DatosEstadoCuenta datos = new EstadoCuentaRepository(app).cargarSync(rango, ahora);
            GeneradorEstadoCuenta g = new GeneradorEstadoCuenta(app);
            File pdf = g.generar(datos);
            assertTrue(pdf.exists());
            assertTrue(pdf.length() > 1000);
            assertTrue(g.getPaginas() >= 1);
            assertTrue(pdf.getName().startsWith("Estado_de_cuenta_Abarrotes_Lupita_Hijos_"));
            byte[] inicio = new byte[5];
            try (FileInputStream in = new FileInputStream(pdf)) {
                assertEquals(5, in.read(inicio));
            }
            assertEquals("%PDF-", new String(inicio, "US-ASCII"));
        }
    }
}
