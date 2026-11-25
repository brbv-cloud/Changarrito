package com.changarrito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.changarrito.utils.ClaveUtil;

import org.junit.Test;

public class ClaveUtilTest {

    @Test
    public void clave_correcta_se_verifica() {
        String sal = ClaveUtil.generarSal();
        String hash = ClaveUtil.hashear("4821", sal);
        assertTrue(ClaveUtil.verificar("4821", sal, hash));
    }

    @Test
    public void clave_incorrecta_no_se_verifica() {
        String sal = ClaveUtil.generarSal();
        String hash = ClaveUtil.hashear("4821", sal);
        assertFalse(ClaveUtil.verificar("4822", sal, hash));
    }

    @Test
    public void misma_clave_con_distinta_sal_da_distinto_hash() {
        assertNotEquals(ClaveUtil.hashear("1234", ClaveUtil.generarSal()),
                ClaveUtil.hashear("1234", ClaveUtil.generarSal()));
    }

    @Test
    public void solo_acepta_cuatro_digitos() {
        assertTrue(ClaveUtil.esClaveValida("0042"));
        assertFalse(ClaveUtil.esClaveValida("123"));
        assertFalse(ClaveUtil.esClaveValida("12345"));
        assertFalse(ClaveUtil.esClaveValida("12a4"));
        assertFalse(ClaveUtil.esClaveValida(null));
    }

    @Test
    public void datos_nulos_no_verifican() {
        assertFalse(ClaveUtil.verificar(null, "00", "00"));
    }
}
