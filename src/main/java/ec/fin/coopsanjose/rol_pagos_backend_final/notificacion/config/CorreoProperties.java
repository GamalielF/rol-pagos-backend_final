package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Cómo sale el correo. En producción la URL y el token llegan desde Vault (correo.api.url, correo.api.token). */
@ConfigurationProperties(prefix = "app.correo")
public record CorreoProperties(
        @DefaultValue("API") Modo modo,
        @DefaultValue("OK") ResultadoSimulado resultadoSimulado,
        @DefaultValue Api api) {

    public enum Modo {
        API,
        SIMULADO
    }

    public enum ResultadoSimulado {
        OK,
        FALLO_TEMPORAL,
        FALLO_DEFINITIVO
    }

    public record Api(
            String url,
            String token,
            @DefaultValue("Authorization") String cabeceraAutorizacion,
            @DefaultValue("Bearer") String esquema,
            String cabeceraIdempotencia,
            @DefaultValue("5s") Duration timeoutConexion,
            @DefaultValue("15s") Duration timeoutLectura,
            String sslBundle) {
    }
}
