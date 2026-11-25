package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.CatalogoBarcodesHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Prueba instrumentada porque CatalogoBarcodesHelper necesita un Context real
 * (AssetManager + SQLiteDatabase) para copiar y abrir catalogo_barcodes.db.
 */
@RunWith(AndroidJUnit4.class)
public class CatalogoBarcodesHelperTest {

    @Test
    public void barcodeConocido_devuelveNombre() {
        Context context = ApplicationProvider.getApplicationContext();
        CatalogoBarcodesHelper helper = CatalogoBarcodesHelper.getInstance(context);

        String nombre = helper.buscarNombre("0000103227240");

        assertEquals("Ositos Tostadas 50g", nombre);
    }

    @Test
    public void barcodeInexistente_devuelveNull() {
        Context context = ApplicationProvider.getApplicationContext();
        CatalogoBarcodesHelper helper = CatalogoBarcodesHelper.getInstance(context);

        String nombre = helper.buscarNombre("0000000000000-no-existe");

        assertNull(nombre);
    }

    @Test
    public void barcodeNuloOVacio_devuelveNullSinTronar() {
        Context context = ApplicationProvider.getApplicationContext();
        CatalogoBarcodesHelper helper = CatalogoBarcodesHelper.getInstance(context);

        assertNull(helper.buscarNombre(null));
        assertNull(helper.buscarNombre(""));
    }
}
