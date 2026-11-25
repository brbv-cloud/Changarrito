package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.changarrito.auth.MensajesErrorAuth;
import com.changarrito.auth.ValidadorAcceso;

import org.junit.Test;

public class ValidadorAccesoTest {

    @Test
    public void correo_valido_e_invalido() {
        assertNull(ValidadorAcceso.validarCorreo("bruno@ceti.mx"));
        assertNull(ValidadorAcceso.validarCorreo("  bruno@ceti.mx  "));
        assertNotNull(ValidadorAcceso.validarCorreo(""));
        assertNotNull(ValidadorAcceso.validarCorreo("bruno"));
        assertNotNull(ValidadorAcceso.validarCorreo("bruno@ceti"));
        assertNotNull(ValidadorAcceso.validarCorreo("bru no@ceti.mx"));
    }

    @Test
    public void contrasena_nueva_minimo_8() {
        assertNotNull(ValidadorAcceso.validarContrasenaNueva("1234567"));
        assertNull(ValidadorAcceso.validarContrasenaNueva("12345678"));
        assertNotNull(ValidadorAcceso.validarContrasenaNueva("        "));
        assertNotNull(ValidadorAcceso.validarContrasenaNueva(null));
    }

    @Test
    public void login_solo_pide_que_no_este_vacia() {
        assertNull(ValidadorAcceso.validarContrasenaLogin("abc"));
        assertNotNull(ValidadorAcceso.validarContrasenaLogin(""));
    }

    @Test
    public void confirmacion_debe_coincidir() {
        assertNull(ValidadorAcceso.validarConfirmacion("secreto123", "secreto123"));
        assertEquals("Las contraseñas no coinciden.",
                ValidadorAcceso.validarConfirmacion("secreto123", "secreto124"));
        assertNotNull(ValidadorAcceso.validarConfirmacion("secreto123", ""));
    }

    @Test
    public void nombre_obligatorio() {
        assertNull(ValidadorAcceso.validarNombre("Bruno"));
        assertNotNull(ValidadorAcceso.validarNombre("   "));
        assertNotNull(ValidadorAcceso.validarNombre("B"));
    }

    @Test
    public void errores_de_firebase_en_espanol() {
        assertEquals("Correo o contraseña incorrectos.", MensajesErrorAuth.porCodigo("ERROR_INVALID_CREDENTIAL"));
        assertEquals("Correo o contraseña incorrectos.", MensajesErrorAuth.porCodigo("ERROR_WRONG_PASSWORD"));
        assertEquals(MensajesErrorAuth.SIN_CONEXION, MensajesErrorAuth.porCodigo("ERROR_NETWORK_REQUEST_FAILED"));
        assertEquals(MensajesErrorAuth.GENERICO, MensajesErrorAuth.porCodigo("ALGO_RARO"));
        assertEquals(MensajesErrorAuth.GENERICO, MensajesErrorAuth.porCodigo(null));
    }
}
