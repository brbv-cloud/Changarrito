package com.changarrito.respaldo;

/** Error de respaldo con el motivo ya clasificado para mostrarle un mensaje claro al usuario. */
public class RespaldoException extends Exception {

    public enum Tipo {
        SIN_INTERNET,      // no hay conexion o se agoto el tiempo
        SIN_SESION,        // no hay cuenta iniciada o el correo no esta verificado
        SIN_PERMISO,       // Firestore rechazo la operacion (reglas)
        SIN_RESPALDO,      // la cuenta nunca ha respaldado
        INCOMPLETO,        // el respaldo descargado no coincide con su resumen
        DESCONOCIDO
    }

    public final Tipo tipo;

    public RespaldoException(Tipo tipo, Throwable causa) {
        super(tipo.name(), causa);
        this.tipo = tipo;
    }
}
