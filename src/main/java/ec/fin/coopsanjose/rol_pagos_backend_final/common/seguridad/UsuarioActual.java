package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** Vacío si no hay sesión (jobs, migraciones) o si es anónima. */
    public static Optional<UsuarioAutenticado> obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }
}