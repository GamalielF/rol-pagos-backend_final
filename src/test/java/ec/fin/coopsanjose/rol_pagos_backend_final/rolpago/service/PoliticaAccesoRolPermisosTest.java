package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Permiso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver roles ajenos es un permiso explícito: lo decide el Admin, no se hereda del rol. */
class PoliticaAccesoRolPermisosTest {

    private final PoliticaAccesoRol politica = new PoliticaAccesoRol();

    private static UsuarioAutenticado usuario(Set<Rol> roles, Set<Permiso> permisos) {
        return new UsuarioAutenticado("zz.prueba", null, null, roles, permisos);
    }

    @Test
    void elAdminSiempreVeRolesAjenosAunSinElPermisoExplicito() {
        assertThat(politica.puedeConsultarAjeno(usuario(Set.of(Rol.ADMIN), Set.of()))).isTrue();
    }

    @Test
    void rrhhConElPermisoVeRolesAjenos() {
        assertThat(politica.puedeConsultarAjeno(usuario(Set.of(Rol.RRHH), Set.of(Permiso.VER_ROLES_AJENOS)))).isTrue();
    }

    @Test
    void rrhhSinElPermisoNoVeRolesAjenos() {
        assertThat(politica.puedeConsultarAjeno(usuario(Set.of(Rol.RRHH), Set.of()))).isFalse();
    }

    @Test
    void elPermisoSinElRolRrhhNoAlcanza() {
        assertThat(politica.puedeConsultarAjeno(usuario(Set.of(Rol.EMPLEADO), Set.of(Permiso.VER_ROLES_AJENOS)))).isFalse();
    }
}