package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}