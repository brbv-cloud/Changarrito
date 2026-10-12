package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.changarrito.bitacora.CambiosProducto;
import com.changarrito.bitacora.DescripcionBitacora;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import org.junit.Test;

import java.util.List;

public class BitacoraTest {

    private ProductoEntity producto() {
        ProductoEntity p = new ProductoEntity();
        p.id = 7;
        p.nombre = "Coca-Cola 600 ml";
        p.categoria = "Bebidas";
        p.precio = 15;
        p.costoCompra = 12;
        p.cantidadDisponible = 24;
        p.unidadMedida = UnidadMedida.PZA;
        return p;
    }

    @Test
    public void sin_cambios_no_registra_nada() {
        assertTrue(CambiosProducto.alEditar(producto(), producto(), 1, 1000).isEmpty());
    }

    @Test
    public void cambio_de_precio_guarda_antes_y_despues() {
        ProductoEntity despues = producto();
        despues.precio = 17;
        List<BitacoraEntity> l = CambiosProducto.alEditar(producto(), despues, 3, 1000);
        assertEquals(1, l.size());
        BitacoraEntity b = l.get(0);
        assertEquals(CambiosProducto.CAMPO_PRECIO, b.campo);
        assertEquals("15.00", b.valorAnterior);
        assertEquals("17.00", b.valorNuevo);
        assertEquals(Integer.valueOf(3), b.idUsuario);
        assertEquals(Integer.valueOf(7), b.idProducto);
        assertEquals(AccionBitacora.EDITAR_PRODUCTO, b.accion);
    }

    @Test
    public void varios_cambios_dan_una_entrada_por_campo() {
        ProductoEntity despues = producto();
        despues.precio = 16;
        despues.costoCompra = 13;
        despues.cantidadDisponible = 20;
        List<BitacoraEntity> l = CambiosProducto.alEditar(producto(), despues, null, 1000);
        assertEquals(3, l.size());
        assertEquals(AccionBitacora.AJUSTAR_EXISTENCIA, l.get(2).accion);
        assertEquals("24", l.get(2).valorAnterior);
        assertEquals("20", l.get(2).valorNuevo);
        assertNull(l.get(0).idUsuario);
    }

    @Test
    public void diferencias_de_redondeo_no_cuentan_como_cambio() {
        ProductoEntity despues = producto();
        despues.precio = 15.00000001;
        assertTrue(CambiosProducto.alEditar(producto(), despues, 1, 1000).isEmpty());
    }

    @Test
    public void frases_para_el_dueno() {
        assertEquals("Ana cambió el precio de Coca-Cola 600 ml: $15.00 → $17.00",
                DescripcionBitacora.texto("Ana", AccionBitacora.EDITAR_PRODUCTO, CambiosProducto.CAMPO_PRECIO,
                        "Coca-Cola 600 ml", "15.00", "17.00"));
        assertEquals("Bruno ajustó la existencia de Arroz: 10 → 7.5",
                DescripcionBitacora.texto("Bruno", AccionBitacora.AJUSTAR_EXISTENCIA,
                        CambiosProducto.CAMPO_EXISTENCIA, "Arroz", "10", "7.5"));
        assertEquals("Alguien eliminó Sal",
                DescripcionBitacora.texto(null, AccionBitacora.ELIMINAR_PRODUCTO, null, "Sal", null, null));
        assertEquals("Ana dio de alta Pluma azul",
                DescripcionBitacora.texto("Ana", AccionBitacora.CREAR_PRODUCTO, null, "Pluma azul", null, null));
    }
}
