package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import org.springframework.http.HttpStatus;

/** Demasiados intentos fallidos (429). */
public class DemasiadosIntentosException extends NegocioException {

    public DemasiadosIntentosException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Espere unos minutos antes de volver a intentarlo");
    }
}
