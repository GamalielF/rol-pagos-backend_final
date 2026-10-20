package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

/** Falla al enviar. El mensaje es una categoría segura: nunca incluye direcciones ni detalles internos. */
public class EnvioCorreoException extends RuntimeException {

    private final boolean reintentable;

    public EnvioCorreoException(String mensaje, boolean reintentable, Throwable causa) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    public boolean esReintentable() {
        return reintentable;
    }
}