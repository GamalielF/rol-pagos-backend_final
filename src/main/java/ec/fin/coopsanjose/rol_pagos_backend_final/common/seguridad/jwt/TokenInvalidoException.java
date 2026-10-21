package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

/** El token no sirve. La categoría es segura de escribir en el log: nunca trae partes del token. */
public class TokenInvalidoException extends RuntimeException {

    private final String categoria;

    public TokenInvalidoException(String categoria) {
        super(categoria);
        this.categoria = categoria;
    }

    public String categoria() {
        return categoria;
    }
}