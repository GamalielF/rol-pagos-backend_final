package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

public interface EnviadorCorreo {

    /** false si falta configurar el servicio: no se intenta enviar nada y los avisos esperan. */
    boolean estaConfigurado();

    /**
     * @param referencia identificador estable del aviso ("aviso-12"): sirve de clave de idempotencia
     * @throws EnvioCorreoException con un mensaje seguro y si vale la pena reintentar
     */
    void enviar(String destinatario, String asunto, String texto, String referencia);
}
