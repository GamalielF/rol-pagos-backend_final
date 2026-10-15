package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** Base de los errores previstos por las reglas del sistema. Cada hija decide su código HTTP. */
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus estado;

    protected NegocioException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
