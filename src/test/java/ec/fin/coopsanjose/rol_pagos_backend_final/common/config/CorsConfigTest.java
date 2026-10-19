package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    private CorsConfiguration configuracionDe(CorsProperties propiedades) {
        UrlBasedCorsConfigurationSource fuente = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource(propiedades);
        return fuente.getCorsConfigurations().get("/**");
    }

    @Test
    void rechazaElComodin() {
        assertThatThrownBy(() -> config.corsConfigurationSource(new CorsProperties(List.of("*"), List.of("Authorization"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(new CorsProperties(List.of("https://*.coopsanjose.fin.ec"), List.of("Authorization"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaOrigenesSinEsquemaOConBarraFinal() {
        assertThatThrownBy(() -> config.corsConfigurationSource(new CorsProperties(List.of("localhost:4200"), List.of("Authorization"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(new CorsProperties(List.of("http://localhost:4200/"), List.of("Authorization"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinOrigenesDeclaradosNoSePermiteNinguno() {
        CorsConfiguration configuracion = configuracionDe(new CorsProperties(List.of(), List.of("Authorization")));

        assertThat(configuracion.getAllowedOrigins()).isEmpty();
    }

    @Test
    void guardaLosOrigenesYNuncaPermiteCredenciales() {
        CorsConfiguration configuracion = configuracionDe(
                new CorsProperties(List.of("http://localhost:4200"), List.of("Authorization", "Content-Type")));

        assertThat(configuracion.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(configuracion.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "OPTIONS");
        assertThat(configuracion.getAllowCredentials()).isFalse();
    }
}
