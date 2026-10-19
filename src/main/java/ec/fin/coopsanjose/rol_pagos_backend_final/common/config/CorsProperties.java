package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/** Por defecto NO se permite ningún origen: cada entorno declara explícitamente el suyo. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue List<String> origenesPermitidos,
        @DefaultValue({"Authorization", "Content-Type"}) List<String> cabecerasPermitidas) {
}