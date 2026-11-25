package com.changarrito.scanner;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Prueba OpenFoodFactsClient.armarNombre(...), el metodo que arma "Marca Nombre
 * Tamano" evitando repetir informacion que ya viene en el nombre.
 *
 * Instrumentada (no JVM normal) porque la clase tiene un campo estatico que crea
 * un Handler(Looper.getMainLooper()) al cargarse, y eso solo funciona con un
 * runtime de Android real (emulador/dispositivo), no con JUnit puro.
 *
 * Regresion (reportada por Bruno): escanear una Coca-Cola de 600ml guardaba el
 * nombre como "Coca Cola Coca Cola 600ml" en vez de "Coca Cola 600ml", porque
 * normalizar() no quitaba el guion y "Coca-Cola" (marca) nunca hacia match contra
 * "Coca Cola 600ml" (nombre, sin guion).
 */
@RunWith(AndroidJUnit4.class)
public class OpenFoodFactsClientTest {

    @Test
    public void marca_con_guion_no_se_duplica_si_el_nombre_ya_la_incluye_sin_guion() {
        String resultado = OpenFoodFactsClient.armarNombre("Coca-Cola", "Coca Cola 600ml", "");
        assertEquals("Coca Cola 600ml", resultado);
    }

    @Test
    public void marca_con_coma_toma_solo_la_primera_y_no_duplica() {
        String resultado = OpenFoodFactsClient.armarNombre(
                "Bimbo, Grupo Bimbo", "Pan Bimbo blanco", "680g");
        assertEquals("Pan Bimbo blanco 680g", resultado);
    }

    @Test
    public void marca_que_no_esta_en_el_nombre_se_antepone() {
        String resultado = OpenFoodFactsClient.armarNombre("Coca-Cola", "Refresco de cola", "600 ml");
        assertEquals("Coca-Cola Refresco de cola 600 ml", resultado);
    }

    @Test
    public void sin_marca_ni_cantidad_repetidas_se_agregan_igual() {
        String resultado = OpenFoodFactsClient.armarNombre("", "Arroz", "1kg");
        assertEquals("Arroz 1kg", resultado);
    }

    @Test
    public void cantidad_ya_incluida_en_el_nombre_no_se_repite() {
        String resultado = OpenFoodFactsClient.armarNombre("", "Leche entera 1L", "1 L");
        assertEquals("Leche entera 1L", resultado);
    }
}
