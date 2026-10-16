package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** Falla de un sistema externo, como el AD (502). El mensaje es seguro: nunca incluye detalles internos. */
public class ServicioExternoException extends NegocioException {

    public ServicioExternoException(String mensaje) {
        super(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
