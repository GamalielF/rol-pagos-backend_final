package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** Configuración del AD. urlBase y token llegan desde Vault (ad.url-base, ad.token). */
@ConfigurationProperties(prefix = "app.ad")
public record AdProperties(
        String urlBase,
        String token,
        String sslBundle,   // nombre del "SSL bundle" con la CA del AD; vacío = almacén por defecto de Java
        @DefaultValue("5s") Duration timeoutConexion,
        @DefaultValue("30s") Duration timeoutLectura,
        @DefaultValue("coopsanjose.fin.ec") String dominioCorreo,
        @DefaultValue List<String> cuentasExcluidas,
        @DefaultValue Sincronizacion sincronizacion) {

    public record Sincronizacion(
            @DefaultValue("false") boolean habilitada,
            @DefaultValue("0 0 3 * * *") String cron,
            @DefaultValue("50") int porcentajeMinimo) {
    }
}