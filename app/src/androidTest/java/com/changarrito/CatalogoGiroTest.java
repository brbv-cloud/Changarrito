package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.negocio.CatalogoGiro;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Revisa que los catálogos de assets/ se lean bien y sean coherentes con su giro. */
@RunWith(AndroidJUnit4.class)
public class CatalogoGiroTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void cada_giro_con_catalogo_trae_productos_validos() {
        for (GiroNegocio giro : GiroNegocio.values()) {
            if (giro.archivoCatalogo == null) continue;
            CatalogoGiro c = CatalogoGiro.cargar(context, giro);
            assertTrue(giro + " sin productos", c.total() >= 40);
            assertTrue(giro + " sin margen", c.margenSugerido > 0);

            Set<String> nombres = new HashSet<>();
            for (List<ProductoEntity> grupo : c.porCategoria.values()) {
                for (ProductoEntity p : grupo) {
                    assertTrue(p.nombre + " repetido en " + giro, nombres.add(p.nombre));
                    assertTrue(p.nombre + ": unidad " + p.unidadMedida + " no es de " + giro,
                            giro.unidadesPorDefecto.contains(p.unidadMedida));
                    assertTrue(p.nombre + ": precio menor al costo", p.precio >= p.costoCompra);
                    assertEquals(0, p.cantidadDisponible, 0);
                    assertTrue(p.activo);
                }
            }
        }
    }

    @Test
    public void negocio_independiente_no_tiene_catalogo() {
        assertEquals(0, CatalogoGiro.cargar(context, GiroNegocio.INDEPENDIENTE).total());
    }
}
