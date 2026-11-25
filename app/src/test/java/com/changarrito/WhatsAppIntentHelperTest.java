package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.utils.WhatsAppIntentHelper;

import org.junit.Test;

/**
 * Prueba pura de JVM (sin emulador): WhatsAppIntentHelper no depende de
 * Context/Intent, solo arma texto, asi que corre como test unitario normal.
 */
public class WhatsAppIntentHelperTest {

    @Test
    public void construirMensaje_incluyeNombreCantidadYUnidad() {
        String mensaje = WhatsAppIntentHelper.construirMensaje("Frijol", 0.750, UnidadMedida.KG);
        assertTrue(mensaje.contains("Frijol"));
        assertTrue(mensaje.contains("0.75"));
        assertTrue(mensaje.contains("KG"));
    }

    @Test
    public void construirMensaje_cantidadEntera_sinDecimalesSobrantes() {
        String mensaje = WhatsAppIntentHelper.construirMensaje("Coca Cola", 12, UnidadMedida.PZA);
        assertTrue(mensaje.contains("12 PZA"));
    }

    @Test
    public void construirUrl_formatoWaMeCorrecto() {
        String url = WhatsAppIntentHelper.construirUrl("3312345678", "Hola mundo");
        assertTrue(url.startsWith("https://wa.me/3312345678?text="));
        assertTrue(url.contains("Hola+mundo"));
    }

    @Test
    public void construirUrl_telefonoConEspacios_seLimpiaAntes() {
        String url = WhatsAppIntentHelper.construirUrl("  3312345678  ", "hola");
        assertEquals("https://wa.me/3312345678?text=hola", url);
    }

    @Test
    public void construirUrl_telefonoNulo_noTronaYUsaCadenaVacia() {
        String url = WhatsAppIntentHelper.construirUrl(null, "hola");
        assertEquals("https://wa.me/?text=hola", url);
    }
}
