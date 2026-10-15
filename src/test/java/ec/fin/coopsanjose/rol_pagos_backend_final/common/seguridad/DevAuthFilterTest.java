package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevAuthFilterTest {

    private final DevAuthFilter filtro = new DevAuthFilter();

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private Authentication ejecutar(MockHttpServletRequest peticion) throws Exception {
        filtro.doFilter(peticion, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void sinCabeceraNoAutentica() throws Exception {
        assertThat(ejecutar(new MockHttpServletRequest())).isNull();
    }

    @Test
    void conUsuarioYRolesArmaLaSesion() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader(DevAuthFilter.CABECERA_USUARIO, "fsepa");
        peticion.addHeader(DevAuthFilter.CABECERA_ROLES, "rrhh, empleado");
        peticion.addHeader(DevAuthFilter.CABECERA_CEDULA, "0202125712");

        Authentication auth = ejecutar(peticion);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        UsuarioAutenticado usuario = (UsuarioAutenticado) auth.getPrincipal();
        assertThat(usuario.username()).isEqualTo("fsepa");
        assertThat(usuario.cedula()).isEqualTo("0202125712");
        List<String> autoridades = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(autoridades).containsExactlyInAnyOrder("ROLE_RRHH", "ROLE_EMPLEADO");
    }

    @Test
    void sinRolesAsumeEmpleado() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader(DevAuthFilter.CABECERA_USUARIO, "jperez");

        UsuarioAutenticado usuario = (UsuarioAutenticado) ejecutar(peticion).getPrincipal();

        assertThat(usuario.roles()).containsExactly(Rol.EMPLEADO);
    }

    @Test
    void rolDesconocidoSeIgnoraYNuncaEscalaPrivilegios() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader(DevAuthFilter.CABECERA_USUARIO, "jperez");
        peticion.addHeader(DevAuthFilter.CABECERA_ROLES, "SUPERADMIN");

        UsuarioAutenticado usuario = (UsuarioAutenticado) ejecutar(peticion).getPrincipal();

        assertThat(usuario.tieneRol(Rol.ADMIN)).isFalse();
        assertThat(usuario.roles()).containsExactly(Rol.EMPLEADO);
    }
}
