package ec.fin.coopsanjose.rol_pagos_backend_final.config;

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

    public static final String AUDITOR_SISTEMA = "SISTEMA";

    /** Reloj único de la aplicación: las fechas de auditoría no dependen de la zona del servidor. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Guayaquil"));
    }

    @Bean
    public DateTimeProvider dateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }

    /**
     * Quién hizo el cambio. ETAPA 4: se reemplaza por el usuario del SecurityContext,
     * con SISTEMA como respaldo para tareas sin sesión (jobs, migraciones).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(AUDITOR_SISTEMA);
    }
}
