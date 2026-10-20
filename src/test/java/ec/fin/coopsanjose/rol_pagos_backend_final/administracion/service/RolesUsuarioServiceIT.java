package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity.AsignacionRol;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.repository.AsignacionRolRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Permiso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.ResolutorRoles.RolesYPermisos;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RolesUsuarioServiceIT extends SoporteRolesPago {

    @Autowired private RolesUsuarioService servicio;
    @Autowired private AsignacionRolRepository repositorio;

    private AsignacionRol asignar(String usuario, Rol rol, boolean verRolesAjenos) {
        AsignacionRol asignacion = new AsignacionRol();
        asignacion.setUsernameAd(usuario);
        asignacion.setRol(rol);
        asignacion.definirVerRolesAjenos(verRolesAjenos);
        return repositorio.save(asignacion);
    }

    @Test
    void todoUsuarioEsEmpleadoYNadaMas() {
        RolesYPermisos resultado = servicio.resolver("zz.cualquiera");

        assertThat(resultado.roles()).containsExactly(Rol.EMPLEADO);
        assertThat(resultado.permisos()).isEmpty();
    }

    @Test
    void sinNombreSoloEsEmpleado() {
        assertThat(servicio.resolver(null).roles()).containsExactly(Rol.EMPLEADO);
        assertThat(servicio.resolver("   ").roles()).containsExactly(Rol.EMPLEADO);
    }

    @Test
    void losAdministradoresDeArranqueSonAdminSoloPorLaConfiguracion() {
        // No hay ninguna fila en la base de datos para ellos: el rol sale de app.seguridad.administradores-iniciales
        for (String usuario : new String[]{"fsepa", "MSAVALA", "  fsepa "}) {
            RolesYPermisos resultado = servicio.resolver(usuario);

            assertThat(resultado.roles()).containsExactlyInAnyOrder(Rol.ADMIN, Rol.EMPLEADO);
            assertThat(resultado.permisos()).containsExactly(Permiso.VER_ROLES_AJENOS);
        }
    }

    @Test
    void rrhhAsignadoConYSinElPermisoDeVerRolesAjenos() {
        asignar("zz.ana", Rol.RRHH, true);
        asignar("zz.beto", Rol.RRHH, false);

        RolesYPermisos conPermiso = servicio.resolver("zz.ana");
        RolesYPermisos sinPermiso = servicio.resolver("zz.beto");

        assertThat(conPermiso.roles()).containsExactlyInAnyOrder(Rol.EMPLEADO, Rol.RRHH);
        assertThat(conPermiso.permisos()).containsExactly(Permiso.VER_ROLES_AJENOS);
        assertThat(sinPermiso.roles()).containsExactlyInAnyOrder(Rol.EMPLEADO, Rol.RRHH);
        assertThat(sinPermiso.permisos()).isEmpty();
    }

    @Test
    void unAdminAsignadoSiempreTieneElPermiso() {
        asignar("zz.carla", Rol.ADMIN, false);

        RolesYPermisos resultado = servicio.resolver("zz.carla");

        assertThat(resultado.roles()).containsExactlyInAnyOrder(Rol.EMPLEADO, Rol.ADMIN);
        assertThat(resultado.permisos()).containsExactly(Permiso.VER_ROLES_AJENOS);
    }

    @Test
    void unaAsignacionRevocadaYaNoCuenta() {
        AsignacionRol asignacion = asignar("zz.ana", Rol.RRHH, true);
        asignacion.inactivar();
        repositorio.save(asignacion);

        RolesYPermisos resultado = servicio.resolver("zz.ana");

        assertThat(resultado.roles()).containsExactly(Rol.EMPLEADO);
        assertThat(resultado.permisos()).isEmpty();
    }
}
