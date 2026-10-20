package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Sin X-Dev-Roles se usan los roles reales; con la cabecera, solo se simula. */
class DevAuthFilterRolesRealesTest {

    private final Map<String, PerfilDirectorio> directorio = new HashMap<>();
    private final ResolutorRoles resolutor = mock(ResolutorRoles.class);
    private final DevAuthFilter filtro = new DevAuthFilter(
            username -> Optional.ofNullable(directorio.get(username)), resolutor);

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest como(String usuario, String roles) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader(DevAuthFilter.CABECERA_USUARIO, usuario);
        if (roles != null) {
            peticion.addHeader(DevAuthFilter.CABECERA_ROLES, roles);
        }
        return peticion;
    }

    private UsuarioAutenticado ejecutar(MockHttpServletRequest peticion) throws Exception {
        filtro.doFilter(peticion, new MockHttpServletResponse(), new MockFilterChain());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : (UsuarioAutenticado) auth.getPrincipal();
    }

    private PerfilDirectorio perfil(String usuario, boolean activo) {
        return new PerfilDirectorio(usuario, "1712345675", "PEREZ LOPEZ JUAN", usuario + "@coopsanjose.fin.ec", activo);
    }

    @Test
    void sinCabeceraDeRolesUsaLosRolesRealesYSusAutoridades() throws Exception {
        directorio.put("jperez", perfil("jperez", true));
        when(resolutor.resolver("jperez")).thenReturn(new ResolutorRoles.RolesYPermisos(
                Set.of(Rol.EMPLEADO, Rol.RRHH), Set.of(Permiso.VER_ROLES_AJENOS)));

        UsuarioAutenticado usuario = ejecutar(como("jperez", null));

        assertThat(usuario.roles()).containsExactlyInAnyOrder(Rol.EMPLEADO, Rol.RRHH);
        assertThat(usuario.tienePermiso(Permiso.VER_ROLES_AJENOS)).isTrue();
        List<String> autoridades = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        assertThat(autoridades).containsExactlyInAnyOrder("ROLE_EMPLEADO", "ROLE_RRHH");
    }

    @Test
    void losPermisosDelResolutorLleganTalComoSon() throws Exception {
        directorio.put("jperez", perfil("jperez", true));
        when(resolutor.resolver("jperez")).thenReturn(new ResolutorRoles.RolesYPermisos(
                Set.of(Rol.EMPLEADO, Rol.RRHH), Set.of()));   // RRHH sin el permiso de ver roles ajenos

        UsuarioAutenticado usuario = ejecutar(como("jperez", null));

        assertThat(usuario.tieneRol(Rol.RRHH)).isTrue();
        assertThat(usuario.tienePermiso(Permiso.VER_ROLES_AJENOS)).isFalse();
    }

    @Test
    void conCabeceraDeRolesSeSimulaYNoSeConsultaAlResolutor() throws Exception {
        directorio.put("jperez", perfil("jperez", true));

        UsuarioAutenticado usuario = ejecutar(como("jperez", "RRHH"));

        assertThat(usuario.roles()).containsExactly(Rol.RRHH);
        assertThat(usuario.tienePermiso(Permiso.VER_ROLES_AJENOS)).isTrue();   // la simulación conserva el comportamiento de siempre
        verifyNoInteractions(resolutor);
    }

    @Test
    void elResolutorRecibeElCodigoCanonicoDelDirectorio() throws Exception {
        directorio.put("JPEREZ", perfil("jperez", true));
        when(resolutor.resolver("jperez")).thenReturn(new ResolutorRoles.RolesYPermisos(Set.of(Rol.EMPLEADO), Set.of()));

        ejecutar(como("JPEREZ", null));

        verify(resolutor).resolver("jperez");
    }

    @Test
    void unUsuarioInactivoNoLlegaAConsultarSusRoles() throws Exception {
        directorio.put("jperez", perfil("jperez", false));

        assertThat(ejecutar(como("jperez", "ADMIN"))).isNull();
        assertThat(ejecutar(como("jperez", null))).isNull();
        verifyNoInteractions(resolutor);
    }

    @Test
    void unaCabeceraDeRolesEnBlancoCuentaComoAusente() throws Exception {
        when(resolutor.resolver("nuevo")).thenReturn(new ResolutorRoles.RolesYPermisos(Set.of(Rol.EMPLEADO), Set.of()));

        UsuarioAutenticado usuario = ejecutar(como("nuevo", "   "));

        assertThat(usuario.roles()).containsExactly(Rol.EMPLEADO);
        verify(resolutor).resolver("nuevo");
    }
}