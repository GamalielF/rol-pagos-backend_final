package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** El usuario está autenticado pero no puede acceder a ese recurso (403). */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
