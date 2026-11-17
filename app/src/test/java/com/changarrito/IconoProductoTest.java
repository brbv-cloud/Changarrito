package com.changarrito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.changarrito.utils.IconoProducto;

import org.junit.Test;

public class IconoProductoTest {

    @Test
    public void porNombre() {
        assertEquals("🥛", IconoProducto.de("Leche entera 1 L", "Lácteos y refrigerados"));
        assertEquals("🥤", IconoProducto.de("Refresco de cola 600 ml", "Bebidas"));
        assertEquals("🔩", IconoProducto.de("Tornillo para madera 1 pulgada", "Tornillería y fijación"));
        assertEquals("🐱", IconoProducto.de("Arena para gato 5 kg", "Mascotas"));
    }

    @Test
    public void lasEspecificasGananALasGenerales() {
        assertEquals("🧻", IconoProducto.de("Papel higiénico 4 rollos", "Higiene personal"));
        assertEquals("📄", IconoProducto.de("Papel crepé", "Papel"));
        assertEquals("🧴", IconoProducto.de("Agua oxigenada 230 ml", "Curación y primeros auxilios"));
        assertEquals("💧", IconoProducto.de("Agua natural 600 ml", "Bebidas"));
        assertEquals("🌭", IconoProducto.de("Salchicha de pavo 500 g", "Lácteos y refrigerados"));
        assertEquals("🧂", IconoProducto.de("Sal de mesa 1 kg", "Abarrotes básicos"));
        assertEquals("🚰", IconoProducto.de("Pegamento para PVC 125 ml", "Plomería"));
        assertEquals("🔌", IconoProducto.de("Cinta de aislar", "Eléctrico"));
        assertEquals("🎁", IconoProducto.de("Bolsa de regalo mediana", "Regalos y temporada"));
        assertEquals("🧴", IconoProducto.de("Crema corporal 400 ml", "Higiene y cuidado personal"));
        assertEquals("🍫", IconoProducto.de("Chocolate en tableta 180 g", "Abarrotes básicos"));
    }

    @Test
    public void siNoReconoceElNombreUsaLaCategoria() {
        assertEquals("💊", IconoProducto.de("Antiinflamatorio X", "Analgésicos y fiebre"));
        assertEquals("🥫", IconoProducto.de("Cosa rara", "Abarrotes básicos"));
    }

    @Test
    public void sinPistasNoHayIcono() {
        assertNull(IconoProducto.de("Xyz", null));
        assertNull(IconoProducto.de(null, ""));
    }
}
