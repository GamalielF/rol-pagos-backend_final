package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties propiedades) {
        for (String origen : propiedades.origenesPermitidos()) {
            boolean esUrl = origen.startsWith("http://") || origen.startsWith("https://");
            if (!esUrl || origen.contains("*") || origen.endsWith("/")) {
                throw new IllegalStateException("Origen CORS inválido '" + origen
                        + "': debe ser una URL http(s) exacta, sin comodines ni barra final");
            }
        }
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));   // lo único que usa la API
        configuracion.setAllowedHeaders(propiedades.cabecerasPermitidas());
        configuracion.setAllowCredentials(false);                              // el JWT viaja en la cabecera Authorization
        configuracion.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
