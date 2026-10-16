package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/** Configuración de la carga de roles de pago. */
@ConfigurationProperties(prefix = "app.roles")
public record RolPagoProperties(
        @DefaultValue("./data/roles-pago") String rutaBase,
        @DefaultValue("5MB") DataSize tamanoMaximoArchivo,
        @DefaultValue("500") int maxArchivosPorCarga,
        @DefaultValue("1") int maxPaginas) {
}