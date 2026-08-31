package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.viewmodel.ProductoValidator;

import org.junit.Test;

public class ProductoViewModelTest {

    private ProductoEntity productoValido() {
        ProductoEntity p = new ProductoEntity();
        p.nombre = "Arroz";
        p.precio = 25.0;
        p.cantidadDisponible = 3.5;
        p.umbralStockBajo = 1.0;
        p.unidadMedida = UnidadMedida.KG;
        return p;
    }

    @Test
    public void producto_valido_no_da_error() {
        ProductoEntity p = productoValido();
        assertNull(ProductoValidator.validar(p));
    }

    @Test
    public void nombre_vacio_da_error() {
        ProductoEntity p = productoValido();
        p.nombre = "  ";
        assertEquals("El nombre no puede estar vacío", ProductoValidator.validar(p));
    }

    @Test
    public void nombre_null_da_error() {
        ProductoEntity p = productoValido();
        p.nombre = null;
        assertNotNull(ProductoValidator.validar(p));
    }

    @Test
    public void precio_negativo_da_error() {
        ProductoEntity p = productoValido();
        p.precio = -5;
        assertEquals("El precio no puede ser negativo", ProductoValidator.validar(p));
    }

    @Test
    public void cantidad_negativa_da_error() {
        ProductoEntity p = productoValido();
        p.cantidadDisponible = -1;
        assertEquals("La cantidad no puede ser negativa", ProductoValidator.validar(p));
    }

    @Test
    public void umbral_negativo_da_error() {
        ProductoEntity p = productoValido();
        p.umbralStockBajo = -1;
        assertEquals("El umbral de stock bajo no puede ser negativo", ProductoValidator.validar(p));
    }

    @Test
    public void sin_unidad_medida_da_error() {
        ProductoEntity p = productoValido();
        p.unidadMedida = null;
        assertEquals("Debes seleccionar una unidad de medida", ProductoValidator.validar(p));
    }

    @Test
    public void pza_con_decimales_da_error() {
        ProductoEntity p = productoValido();
        p.unidadMedida = UnidadMedida.PZA;
        p.cantidadDisponible = 2.5;
        assertEquals("PZA no acepta cantidades decimales", ProductoValidator.validar(p));
    }

    @Test
    public void pza_con_entero_es_valido() {
        ProductoEntity p = productoValido();
        p.unidadMedida = UnidadMedida.PZA;
        p.cantidadDisponible = 3.0;
        assertNull(ProductoValidator.validar(p));
    }

    @Test
    public void kg_con_decimales_es_valido() {
        ProductoEntity p = productoValido();
        p.unidadMedida = UnidadMedida.KG;
        p.cantidadDisponible = 0.750;
        assertNull(ProductoValidator.validar(p));
    }
}