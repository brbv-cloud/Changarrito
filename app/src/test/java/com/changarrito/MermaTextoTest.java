package com.changarrito;

import static org.junit.Assert.assertEquals;

import com.changarrito.bitacora.DescripcionBitacora;
import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.MotivoMerma;

import org.junit.Test;

public class MermaTextoTest {

    @Test
    public void bitacora_describe_la_merma_con_su_motivo() {
        assertEquals("Ana registró una merma de Coca-Cola (Caducado): 10 PZA → 7 PZA",
                DescripcionBitacora.texto("Ana", AccionBitacora.REGISTRAR_MERMA, "CADUCADO", "Coca-Cola",
                        "10 PZA", "7 PZA"));
    }

    @Test
    public void motivo_desconocido_no_truena() {
        assertEquals("Robo o faltante", MotivoMerma.nombre("ROBO"));
        assertEquals("Otro", MotivoMerma.nombre(null));
        assertEquals("ALGO_NUEVO", MotivoMerma.nombre("ALGO_NUEVO"));
    }
}
