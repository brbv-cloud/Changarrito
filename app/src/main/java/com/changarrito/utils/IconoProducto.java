package com.changarrito.utils;

/**
 * Dibujo (emoji) para la tarjeta de un producto, segun su nombre o su categoria:
 * 🥛 leche, 🥤 refresco, 🔩 tornillo, ✏️ lapiz... Asi se reconoce de un vistazo sin
 * fotos (que ocuparian espacio y respaldo) y funciona sin internet.
 *
 * Primero se busca por el NOMBRE (mas preciso) y si no, por la CATEGORIA. El orden de
 * las reglas importa: las especificas van antes ("papel higienico" antes que "papel").
 * Si nada coincide devuelve null y la tarjeta muestra la inicial, como antes.
 */
public final class IconoProducto {

    private IconoProducto() {
    }

    /** {emoji, palabra1, palabra2, ...}: si el nombre contiene alguna palabra, ese emoji. */
    private static final String[][] POR_NOMBRE = {
            // mascotas y bebe (antes que "toallas", "crema"...)
            {"🐶", "perro"}, {"🐱", "gato"},
            {"🍫", "chocolate"}, // antes que "tableta" (farmacia)
            {"🍼", "biberon", "chupon"}, {"👶", "panal", "bebe", "rozadura"},
            // farmacia y cuidado personal
            {"🌡️", "termometro"}, {"💉", "jeringa"}, {"😷", "cubrebocas"},
            {"🩹", "curita", "venda", "gasa", "algodon", "microporosa"},
            {"🧴", "agua oxigenada", "alcohol", "antiseptico", "antibacterial", "bloqueador", "corporal",
                    "pomada", "shampoo", "desodorante", "silicon", "resistol", "adhesivo 20",
                    "espuma", "enjuague"},
            {"🦷", "dental", "cepillo"}, {"🪒", "rastrillo"}, {"🌸", "femenina"},
            {"🧻", "higienico", "toallitas"},
            {"💧", "agua natural", "garrafon", "suero", "electrolito"},
            {"💊", "tableta", "jarabe", "pastilla", "gragea", "vitamina", "complejo b", "loratadina",
                    "antigripal", "analgesico", "antiacido", "sales de rehidratacion", "vaporub", "spray",
                    "salina", "subsalicilato", "preservativo", "prueba de embarazo"},
            // bebidas
            {"🍺", "cerveza"}, {"☕", "cafe"}, {"🧃", "jugo"}, {"🥤", "refresco", "rehidratante"},
            // lacteos y refrigerados
            {"🥛", "leche", "yogurt", "crema"}, {"🧀", "queso"}, {"🥓", "jamon"}, {"🌭", "salchicha"},
            {"🧈", "mantequilla"}, {"🥚", "huevo"},
            // pan y tortilla
            {"🥐", "pan dulce", "concha"}, {"🍞", "pan de caja", "pan blanco"}, {"🌮", "tortilla", "tostada"},
            // botanas y dulces
            {"🍟", "papas fritas"}, {"🌽", "frituras", "harina de maiz"}, {"🥜", "cacahuate"},
            {"🍪", "galleta"}, {"🧁", "pastelito"}, {"🍭", "paleta"},
            {"🍬", "chicle", "dulce"},
            // abarrotes
            {"🍚", "arroz"}, {"🌾", "frijol", "lenteja", "avena"}, {"🧂", "sal de", "consome"},
            {"🥄", "azucar"}, {"🍳", "aceite"}, {"🐟", "atun"}, {"🍝", "sopa", "spaghetti", "pasta 2"},
            {"🍅", "tomate"}, {"🌶️", "chile"}, {"🥫", "mayonesa", "lata"},
            // limpieza
            {"🧼", "jabon", "detergente", "cloro", "limpiador", "lavatrastes"}, {"🧽", "fibra"},
            {"🗑️", "basura"},
            // varios de tienda
            {"🔋", "pila"}, {"🔥", "encendedor", "cerillo"}, {"🕯️", "veladora"}, {"⛽", "gas butano"},
            // ferreteria
            {"🚰", "pvc", "cpvc", "teflon", "llave de paso", "flotador", "cespol", "sapito", "lavabo"},
            {"🧴", "pegamento"}, // antes que "contacto" (pegamento de contacto)
            {"🔌", "cable", "apagador", "contacto", "clavija", "extension", "multicontacto", "centro de carga",
                    "interruptor", "aislar"},
            {"💡", "foco", "socket"},
            {"🌱", "jardin", "llave de nariz"},
            {"🔩", "tornillo", "taquete", "clavo", "pija", "tuerca", "rondana", "broca", "alambre"},
            {"🔨", "martillo"}, {"📏", "flexometro"}, {"📐", "nivel", "geometria", "regla"},
            {"🔪", "cuter", "navaja"},
            {"🔧", "desarmador", "pinzas", "llave", "segueta", "lija"},
            {"🎨", "pintura", "esmalte", "thinner", "brocha", "rodillo", "sellador", "acuarela", "pincel",
                    "plastilina"},
            {"🧱", "cemento", "yeso", "cal hidratada", "pegazulejo", "resanador", "impermeabilizante"},
            {"🔒", "candado", "chapa"}, {"⛓️", "cadena"}, {"🚪", "bisagra"}, {"🧹", "escoba", "recogedor", "cubeta"},
            {"🧤", "guante"},
            // papeleria
            {"💌", "felicitacion"}, {"🎁", "regalo", "envolver", "mono"}, {"🎈", "globo"},
            {"✏️", "lapiz", "lapicero", "puntilla", "goma", "sacapuntas"},
            {"🖊️", "pluma", "corrector"}, {"🖍️", "plumon", "marcatextos", "plumin", "colores", "crayola"},
            {"📓", "cuaderno", "libreta"}, {"📝", "notas adhesivas"}, {"✨", "diamantina"}, {"✂️", "tijera"},
            {"📁", "folder", "sobre", "carpeta"}, {"📎", "clip", "grapa", "engrapadora"},
            {"📦", "cinta", "masking"}, {"🧮", "calculadora"}, {"🏷️", "tarjeta", "etiqueta"},
            {"🖨️", "copia", "impresion", "escaneo", "engargolado", "enmicado"},
            {"📄", "hoja", "papel", "cartulina", "foami"},
    };

    private static final String[][] POR_CATEGORIA = {
            {"🐾", "mascota"}, {"👶", "bebe"}, {"🩹", "curacion"},
            {"💊", "analgesico", "gripa", "estomacal", "vitamina", "medicamento", "farmacia"},
            {"🧴", "higiene", "cuidado personal"}, {"🧽", "limpieza"},
            {"🥛", "lacteo", "refrigerado"}, {"🍞", "panaderia", "tortilla"}, {"🥤", "bebida"},
            {"🍬", "botana", "dulce"}, {"🌾", "grano", "semilla"}, {"🥫", "abarrote"},
            {"🔩", "tornilleria", "fijacion"}, {"🔌", "electrico"}, {"🚰", "plomeria"}, {"🔧", "herramienta"},
            {"🎨", "pintura", "acabado", "arte", "manualidad"}, {"🧱", "construccion"}, {"🌱", "jardin"},
            {"✏️", "escritura"}, {"📓", "cuaderno"}, {"📄", "papel"}, {"📎", "oficina"},
            {"🖨️", "servicio"}, {"🎁", "regalo", "temporada"},
    };

    /** El emoji del producto, o null si no se reconoce (entonces va la inicial). */
    public static String de(String nombre, String categoria) {
        String porNombre = buscar(POR_NOMBRE, EstadoProducto.normalizar(nombre));
        if (porNombre != null) return porNombre;
        return buscar(POR_CATEGORIA, EstadoProducto.normalizar(categoria));
    }

    private static String buscar(String[][] reglas, String texto) {
        if (texto.isEmpty()) return null;
        for (String[] regla : reglas) {
            for (int i = 1; i < regla.length; i++) {
                if (texto.contains(regla[i])) return regla[0];
            }
        }
        return null;
    }
}
