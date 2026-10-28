package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * La API de correo para un solo destinatario (por ejemplo .../notificaciones/exchange/send-prueba?destinatario=...).
 * Se usa para reenviar un aviso a una persona. Comparte con el envío masivo el token, las cabeceras, los plazos y el
 * certificado. Si la URL está vacía, el reenvío usa el envío masivo con un solo aviso. En producción la URL llega desde
 * Vault (correo.individual.url).
 */
public record CorreoIndividualProperties(
        String url,
        @DefaultValue("POST") String metodo,
        @DefaultValue("destinatario") String parametroDestinatario) {

    public static final CorreoIndividualProperties SIN_CONFIGURAR = new CorreoIndividualProperties(null, "POST", "destinatario");

    /** Lee app.correo.individual del entorno; sin nada configurado, el reenvío usa el envío masivo. */
    public static CorreoIndividualProperties desde(Environment entorno) {
        if (entorno instanceof ConfigurableEnvironment configurable) {
            return Binder.get(configurable).bind("app.correo.individual", CorreoIndividualProperties.class).orElse(SIN_CONFIGURAR);
        }
        return SIN_CONFIGURAR;
    }

    public boolean configurada() {
        return url != null && !url.isBlank();
    }
}
