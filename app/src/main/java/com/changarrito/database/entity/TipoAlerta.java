package com.changarrito.database.entity;

/**
 * STOCK_BAJO: queda poco (en o bajo el minimo). AGOTADO: ya no hay; reemplaza a la de
 * stock bajo para avisar otra vez cuando se acaba. PROXIMO_VENCIMIENTO: caduca pronto.
 * (Se guarda por nombre en la BD: agregar valores no requiere migracion.)
 */
public enum TipoAlerta {
    STOCK_BAJO, PROXIMO_VENCIMIENTO, AGOTADO
}
