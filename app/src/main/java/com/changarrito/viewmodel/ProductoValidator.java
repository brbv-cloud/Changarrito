package com.changarrito.viewmodel;

import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

public class ProductoValidator {

    /** Devuelve null si es válido, o un mensaje de error si no. */
    public static String validar(ProductoEntity producto) {
        if (producto.nombre == null || producto.nombre.trim().isEmpty()) {
            return "El nombre no puede estar vacío";
        }
        if (producto.precio < 0) {
            return "El precio no puede ser negativo";
        }
        if (producto.cantidadDisponible < 0) {
            return "La cantidad no puede ser negativa";
        }
        if (producto.umbralStockBajo < 0) {
            return "El umbral de stock bajo no puede ser negativo";
        }
        if (producto.unidadMedida == null) {
            return "Debes seleccionar una unidad de medida";
        }
        if (producto.unidadMedida == UnidadMedida.PZA
                && producto.cantidadDisponible != Math.floor(producto.cantidadDisponible)) {
            return "PZA no acepta cantidades decimales";
        }
        return null;
    }
}