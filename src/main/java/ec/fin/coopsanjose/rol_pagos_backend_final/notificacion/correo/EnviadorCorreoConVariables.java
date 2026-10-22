package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import java.util.Map;

/** Un enviador que acepta datos adicionales por aviso (las variables de la plantilla de la API de correo). */
public interface EnviadorCorreoConVariables extends EnviadorCorreo {

    void enviar(String destinatario, String asunto, String texto, String referencia, Map<String, String> variables);
}