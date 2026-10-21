package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.seguridad.limite-intentos")
public record LimiteIntentosProperties(@DefaultValue("5") int maximo, @DefaultValue("15m") Duration ventana) {
}
