package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class SecurityContextAuditorAware implements AuditorAware<String> {

    /** Auditor de respaldo para tareas sin sesión: jobs, migraciones, arranque. */
    public static final String SISTEMA = "SISTEMA";

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(UsuarioActual.obtener().map(UsuarioAutenticado::username).orElse(SISTEMA));
    }
}
