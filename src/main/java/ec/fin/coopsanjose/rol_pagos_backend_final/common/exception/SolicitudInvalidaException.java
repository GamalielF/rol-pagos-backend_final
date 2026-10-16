package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** El cliente envió algo que no se puede procesar (400). */
public class SolicitudInvalidaException extends NegocioException {

    public SolicitudInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
