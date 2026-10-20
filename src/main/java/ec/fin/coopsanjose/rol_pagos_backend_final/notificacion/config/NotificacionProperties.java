package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.notificaciones")
public record NotificacionProperties(
        @DefaultValue("false") boolean habilitada,
        @DefaultValue("true") boolean despachoAutomatico,
        @DefaultValue("50") int lote,
        @DefaultValue("5") int maxIntentos,
        String urlSistema) {
}
