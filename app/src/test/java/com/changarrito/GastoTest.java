package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.changarrito.bitacora.DescripcionBitacora;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.TipoGasto;

import org.junit.Test;

import java.util.List;

public class GastoTest {

    @Test
    public void el_trabajador_no_ve_renta_ni_sueldos() {
        List<TipoGasto> trabajador = TipoGasto.disponibles(false);
        assertFalse(trabajador.contains(TipoGasto.RENTA));
        assertFalse(trabajador.contains(TipoGasto.SUELDOS));
        assertTrue(trabajador.contains(TipoGasto.AGUA));
        assertEquals(TipoGasto.values().length, TipoGasto.disponibles(true).size());
    }

    @Test
    public void nombre_tolera_valores_desconocidos() {
        assertEquals("Luz", TipoGasto.nombre("LUZ"));
        assertEquals("Otro", TipoGasto.nombre(null));
        assertEquals("XYZ", TipoGasto.nombre("XYZ"));
    }

    @Test
    public void bitacora_dice_que_salio_de_la_caja() {
        assertEquals("Ana registró un gasto de Agua: $40.00, pagado con la caja (garrafón)",
                DescripcionBitacora.texto("Ana", AccionBitacora.REGISTRAR_GASTO, "AGUA", "garrafón", "caja", "$40.00"));
        assertEquals("Bruno borró un gasto de Renta: $3,500.00",
                DescripcionBitacora.texto("Bruno", AccionBitacora.BORRAR_GASTO, "RENTA", null, null, "$3,500.00"));
    }
}
