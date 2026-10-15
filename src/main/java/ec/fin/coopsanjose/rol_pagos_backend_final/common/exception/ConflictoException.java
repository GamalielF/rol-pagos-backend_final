package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** Choca con el estado actual: duplicado, periodo ya publicado, etc. (409). */
public class ConflictoException extends NegocioException {

    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
