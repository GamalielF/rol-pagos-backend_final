package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

/** Operación prohibida por las reglas del sistema (ej. borrar un registro).**/
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}