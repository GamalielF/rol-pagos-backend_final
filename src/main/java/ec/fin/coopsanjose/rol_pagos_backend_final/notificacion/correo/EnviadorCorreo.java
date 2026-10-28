package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import java.util.Map;

public interface EnviadorCorreo {

    /** false si falta configurar el servicio: no se intenta enviar nada y los avisos esperan. */
    boolean estaConfigurado();

    /**
     * @param referencia identificador estable del aviso ("aviso-12"): sirve de clave de idempotencia
     * @throws EnvioCorreoException con un mensaje seguro y si vale la pena reintentar
     */
    void enviar(String destinatario, String asunto, String texto, String referencia);

    /**
     * Reenvío puntual de un aviso a una persona. Por defecto es el mismo envío de siempre; el adaptador de la API de
     * correo lo cambia por su servicio individual cuando está configurado.
     */
    default void reenviar(String destinatario, String asunto, String texto, String referencia,
                          Map<String, String> variables) {
        enviar(destinatario, asunto, texto, referencia);
    }
}
