package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.AlertaEntity;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;
import com.changarrito.respaldo.RespaldoCompleto;
import com.changarrito.respaldo.RespaldoLocal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Viaje completo en la BD local: se llena un negocio, se toma la "foto", se
 * ensucia la BD y se restaura. Todo debe quedar igual, con los mismos ids.
 * (La parte de Firestore se prueba a mano en el celular: necesita internet y cuenta.)
 */
@RunWith(AndroidJUnit4.class)
public class RespaldoLocalTest {

    private AppDatabase db;
    private int idAna, idCoca, idFrijol;

    @Before
    public void crearBD() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        llenarNegocio();
    }

    @After
    public void cerrarBD() {
        db.close();
    }

    private void llenarNegocio() {
        ConfiguracionNegocioEntity c = ConfiguracionNegocioEntity.paraGiro(GiroNegocio.ABARROTES, "uid-viejo", 30);
        c.nombreNegocio = "Abarrotes Lupita";
        c.catalogoImportado = true;
        db.configuracionNegocioDao().guardar(c);

        UsuarioEntity dueno = usuario("Bruno", Rol.DUENO);
        db.usuarioDao().insert(dueno);
        UsuarioEntity ana = usuario("Ana", Rol.TRABAJADOR);
        ana.claveHash = "hash";
        ana.claveSal = "sal";
        idAna = (int) db.usuarioDao().insert(ana);

        idCoca = (int) db.productoDao().insert(producto("Coca-Cola 600 ml", UnidadMedida.PZA, 17, 12));
        idFrijol = (int) db.productoDao().insert(producto("Frijol", UnidadMedida.KG, 40, 30));

        VentaEntity v = new VentaEntity();
        v.idProducto = idFrijol;
        v.cantidadVendida = 0.75;
        v.unidadMedida = UnidadMedida.KG;
        v.precioUnitarioVenta = 40;
        v.costoUnitarioVenta = 30;
        v.total = 30;
        v.timestampVentaMs = 1000;
        v.idUsuario = idAna;
        db.ventaDao().insert(v);

        VentaEntity sinTurno = new VentaEntity();
        sinTurno.idProducto = idCoca;
        sinTurno.cantidadVendida = 1;
        sinTurno.unidadMedida = UnidadMedida.PZA;
        sinTurno.total = 17;
        sinTurno.timestampVentaMs = 2000;
        sinTurno.idUsuario = null;
        db.ventaDao().insert(sinTurno);

        CierreCajaEntity cierre = new CierreCajaEntity();
        cierre.fechaMs = 0;
        cierre.efectivoSistema = 47;
        cierre.efectivoFisicoContado = 45;
        cierre.diferencia = -2;
        cierre.notas = "cambio mal dado";
        cierre.timestampCreacionMs = 3000;
        cierre.idUsuario = idAna;
        db.cierreCajaDao().insert(cierre);

        BitacoraEntity b = new BitacoraEntity();
        b.timestampMs = 4000;
        b.idUsuario = idAna;
        b.idProducto = idCoca;
        b.nombreProducto = "Coca-Cola 600 ml";
        b.accion = AccionBitacora.EDITAR_PRODUCTO;
        b.campo = "precio";
        b.valorAnterior = "15.00";
        b.valorNuevo = "17.00";
        db.bitacoraDao().insert(b);
    }

    private static UsuarioEntity usuario(String nombre, Rol rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.nombre = nombre;
        u.rol = rol;
        u.timestampCreacionMs = 1;
        return u;
    }

    private static ProductoEntity producto(String nombre, UnidadMedida unidad, double precio, double costo) {
        ProductoEntity p = new ProductoEntity();
        p.nombre = nombre;
        p.precio = precio;
        p.costoCompra = costo;
        p.cantidadDisponible = 10.5;
        p.unidadMedida = unidad;
        p.umbralStockBajo = 2;
        p.telefonoProveedor = "3312345678";
        p.barcode = "";
        p.timestampCreacionMs = 1;
        p.timestampActualizacionMs = 1;
        return p;
    }

    @Test
    public void leer_trae_todas_las_tablas() {
        RespaldoCompleto r = RespaldoLocal.leer(db);
        assertEquals("Abarrotes Lupita", r.configuracion.nombreNegocio);
        assertEquals(2, r.usuarios.size());
        assertEquals(2, r.productos.size());
        assertEquals(2, r.ventas.size());
        assertEquals(1, r.cierres.size());
        assertEquals(1, r.bitacora.size());
    }

    @Test
    public void restaurar_deja_todo_igual_con_los_mismos_ids() {
        RespaldoCompleto foto = RespaldoLocal.leer(db);

        // Se "ensucia" la BD: otro producto, otra venta, una alerta y se borra la config.
        int idNuevo = (int) db.productoDao().insert(producto("Producto nuevo", UnidadMedida.PZA, 1, 1));
        VentaEntity extra = new VentaEntity();
        extra.idProducto = idNuevo;
        extra.cantidadVendida = 1;
        extra.unidadMedida = UnidadMedida.PZA;
        extra.total = 1;
        extra.timestampVentaMs = 5000;
        db.ventaDao().insert(extra);
        AlertaEntity alerta = new AlertaEntity();
        alerta.idProducto = idCoca;
        alerta.tipoAlerta = TipoAlerta.STOCK_BAJO;
        alerta.estado = EstadoAlerta.ACTIVA;
        db.alertaDao().insert(alerta);

        RespaldoLocal.reemplazar(db, foto, "uid-nuevo");

        RespaldoCompleto despues = RespaldoLocal.leer(db);
        assertEquals(2, despues.productos.size());
        assertEquals(2, despues.ventas.size());
        assertEquals("uid-nuevo", despues.configuracion.uidFirebase);
        assertEquals(GiroNegocio.ABARROTES, despues.configuracion.giro);
        assertTrue(despues.configuracion.catalogoImportado);

        // Mismos ids y relaciones intactas
        UsuarioEntity ana = db.usuarioDao().getUsuarioByIdSync(idAna);
        assertEquals("Ana", ana.nombre);
        assertEquals("hash", ana.claveHash);
        VentaEntity granel = despues.ventas.get(0);
        assertEquals(idFrijol, granel.idProducto);
        assertEquals(0.75, granel.cantidadVendida, 0.0001);
        assertEquals(Integer.valueOf(idAna), granel.idUsuario);
        assertNull(despues.ventas.get(1).idUsuario);
        assertEquals(-2, despues.cierres.get(0).diferencia, 0.0001);
        assertEquals(AccionBitacora.EDITAR_PRODUCTO, despues.bitacora.get(0).accion);

        // Las alertas se recalculan solas: el respaldo las deja vacias.
        assertNull(db.alertaDao().getAlertaExistenteSync(idCoca, TipoAlerta.STOCK_BAJO, EstadoAlerta.ACTIVA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void respaldo_sin_negocio_no_borra_nada() {
        RespaldoLocal.reemplazar(db, new RespaldoCompleto(), "uid");
    }
}
