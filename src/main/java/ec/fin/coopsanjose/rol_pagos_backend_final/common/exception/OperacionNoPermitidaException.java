package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Operación prohibida por las reglas del sistema, por ejemplo borrar un registro (409).
 * El 403 queda reservado a la seguridad (Etapa 4).
 */
public class OperacionNoPermitidaException extends NegocioException {

    public OperacionNoPermitidaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}