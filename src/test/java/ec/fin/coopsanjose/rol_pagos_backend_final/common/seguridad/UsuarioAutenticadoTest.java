package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioAutenticadoTest {

    @Test
    void elConstructorCortoDaElPermisoAPersonalDeRrhhYAdmin() {
        assertThat(new UsuarioAutenticado("u", null, null, Set.of(Rol.RRHH)).tienePermiso(Permiso.VER_ROLES_AJENOS)).isTrue();
        assertThat(new UsuarioAutenticado("u", null, null, Set.of(Rol.ADMIN)).tienePermiso(Permiso.VER_ROLES_AJENOS)).isTrue();
        assertThat(new UsuarioAutenticado("u", null, null, Set.of(Rol.EMPLEADO)).tienePermiso(Permiso.VER_ROLES_AJENOS)).isFalse();
    }

    @Test
    void elConstructorLargoRespetaLosPermisosExplicitos() {
        UsuarioAutenticado rrhhSinPermiso = new UsuarioAutenticado("u", null, null, Set.of(Rol.RRHH), Set.of());

        assertThat(rrhhSinPermiso.tieneRol(Rol.RRHH)).isTrue();
        assertThat(rrhhSinPermiso.tienePermiso(Permiso.VER_ROLES_AJENOS)).isFalse();
    }

    @Test
    void losConjuntosSeCopianYNoSePuedenAlterar() {
        Set<Rol> original = new HashSet<>(Set.of(Rol.EMPLEADO));
        UsuarioAutenticado usuario = new UsuarioAutenticado("u", null, null, original);

        original.add(Rol.ADMIN);   // cambiar el conjunto original no afecta a la sesión

        assertThat(usuario.tieneRol(Rol.ADMIN)).isFalse();
        assertThatThrownBy(() -> usuario.roles().add(Rol.ADMIN)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> usuario.permisos().add(Permiso.VER_ROLES_AJENOS)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void elUsernameEsObligatorio() {
        assertThatThrownBy(() -> new UsuarioAutenticado(null, null, null, Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UsuarioAutenticado("  ", null, null, Set.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void losConjuntosNulosSeVuelvenVacios() {
        UsuarioAutenticado usuario = new UsuarioAutenticado("u", null, null, null, null);

        assertThat(usuario.roles()).isEmpty();
        assertThat(usuario.permisos()).isEmpty();
    }
}
