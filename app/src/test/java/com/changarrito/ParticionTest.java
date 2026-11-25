package com.changarrito;

import static org.junit.Assert.assertEquals;

import com.changarrito.respaldo.Particion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ParticionTest {

    @Test
    public void parte_en_pedazos_y_el_ultimo_lleva_el_resto() {
        List<Integer> filas = new ArrayList<>();
        for (int i = 0; i < 601; i++) filas.add(i);
        List<List<Integer>> partes = Particion.partir(filas, 250);
        assertEquals(3, partes.size());
        assertEquals(250, partes.get(0).size());
        assertEquals(250, partes.get(1).size());
        assertEquals(101, partes.get(2).size());
        assertEquals(Integer.valueOf(600), partes.get(2).get(100));
    }

    @Test
    public void exacto_no_crea_parte_vacia() {
        assertEquals(2, Particion.partir(Arrays.asList(1, 2, 3, 4), 2).size());
    }

    @Test
    public void lista_vacia_o_nula_no_crea_partes() {
        assertEquals(0, Particion.partir(Collections.emptyList(), 250).size());
        assertEquals(0, Particion.partir(null, 250).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tamano_cero_es_error() {
        Particion.partir(Arrays.asList(1, 2), 0);
    }

    @Test
    public void id_de_parte_se_ordena_alfabeticamente() {
        assertEquals("ventas_0000", Particion.idParte("ventas", 0));
        assertEquals("ventas_0012", Particion.idParte("ventas", 12));
        assertEquals(-1, Integer.signum(Particion.idParte("ventas", 9).compareTo(Particion.idParte("ventas", 10))));
    }
}
