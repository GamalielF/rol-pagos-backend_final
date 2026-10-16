package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.SecurityContextAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
public class AuditoriaConfig {

    /** Reloj único de la aplicación: las fechas de auditoría no dependen de la zona del servidor. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Guayaquil"));
    }

    @Bean
    public DateTimeProvider dateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }

    /** Quién hizo el cambio: el usuario de la sesión, o SISTEMA si no hay sesión. */
    @Bean
    public AuditorAware<String> auditorAware() {
        return new SecurityContextAuditorAware();
    }
}
