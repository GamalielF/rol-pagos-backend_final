package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Cómo se valida el token del autenticador. La clave pública NO es secreta, pero cambia por ambiente. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String clavePublica,
        String clavePublicaArchivo,
        @DefaultValue("PT24H5M") Duration vidaMaxima,
        @DefaultValue("60s") Duration desfaseReloj) {
}