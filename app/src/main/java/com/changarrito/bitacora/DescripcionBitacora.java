package com.changarrito.bitacora;

import com.changarrito.database.entity.AccionBitacora;
import com.changarrito.database.entity.MotivoMerma;

/**
 * Convierte una entrada de bitácora en una frase para el dueño, por ejemplo
 * "Ana cambió el precio de Coca-Cola 600 ml: $15.00 → $17.00". Clase pura para JUnit.
 */
public final class DescripcionBitacora {

    private DescripcionBitacora() {
    }

    public static String texto(String quien, AccionBitacora accion, String campo, String producto,
                               String antes, String despues) {
        String persona = quien == null || quien.trim().isEmpty() ? "Alguien" : quien.trim();
        String prod = producto == null ? "un producto" : producto;
        if (accion == null) return persona + " hizo un cambio en " + prod;
        switch (accion) {
            case CREAR_PRODUCTO:
                return persona + " dio de alta " + prod;
            case ELIMINAR_PRODUCTO:
                return persona + " eliminó " + prod;
            case REGISTRAR_MERMA:
                return persona + " registró una merma de " + prod + " (" + MotivoMerma.nombre(campo) + "): "
                        + antes + " → " + despues;
            case AJUSTAR_EXISTENCIA:
                return persona + " ajustó la existencia de " + prod + ": " + antes + " → " + despues;
            default:
                return persona + " cambió " + nombreCampo(campo) + " de " + prod + ": "
                        + formato(campo, antes) + " → " + formato(campo, despues);
        }
    }

    private static String nombreCampo(String campo) {
        if (CambiosProducto.CAMPO_PRECIO.equals(campo)) return "el precio";
        if (CambiosProducto.CAMPO_COSTO.equals(campo)) return "el costo";
        if (CambiosProducto.CAMPO_NOMBRE.equals(campo)) return "el nombre";
        if (CambiosProducto.CAMPO_CATEGORIA.equals(campo)) return "la categoría";
        return "un dato";
    }

    private static String formato(String campo, String valor) {
        if (valor == null || valor.isEmpty()) return "(vacío)";
        if (CambiosProducto.CAMPO_PRECIO.equals(campo) || CambiosProducto.CAMPO_COSTO.equals(campo)) {
            return "$" + valor;
        }
        if (CambiosProducto.CAMPO_NOMBRE.equals(campo) || CambiosProducto.CAMPO_CATEGORIA.equals(campo)) {
            return "\"" + valor + "\"";
        }
        return valor;
    }
}
