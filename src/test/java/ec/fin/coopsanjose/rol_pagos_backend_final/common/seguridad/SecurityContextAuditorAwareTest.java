package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextAuditorAwareTest {

    private final SecurityContextAuditorAware auditor = new SecurityContextAuditorAware();

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinSesionUsaSistema() {
        assertThat(auditor.getCurrentAuditor()).contains("SISTEMA");
    }

    @Test
    void conUsuarioDevuelveSuUsername() {
        UsuarioAutenticado usuario = new UsuarioAutenticado("fsepa", null, null, Set.of(Rol.RRHH));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(Rol.RRHH.autoridad()))));

        assertThat(auditor.getCurrentAuditor()).contains("fsepa");
    }

    @Test
    void sesionAnonimaUsaSistema() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(auditor.getCurrentAuditor()).contains("SISTEMA");
    }
}
