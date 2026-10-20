package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.config.AdministradoresProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity.AsignacionRol;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.repository.AsignacionRolRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Permiso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.ResolutorRoles;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Los roles de un usuario: EMPLEADO para todos, más los administradores de arranque y lo asignado en la base de datos. */
@Service
@RequiredArgsConstructor
public class RolesUsuarioService implements ResolutorRoles {

    private final AsignacionRolRepository repositorio;
    private final AdministradoresProperties administradores;

    @Override
    @Transactional(readOnly = true)
    public RolesYPermisos resolver(String username) {
        Set<Rol> roles = EnumSet.of(Rol.EMPLEADO);
        Set<Permiso> permisos = EnumSet.noneOf(Permiso.class);
        if (username == null || username.isBlank()) {
            return new RolesYPermisos(roles, permisos);
        }
        String usuario = username.trim().toLowerCase(Locale.ROOT);

        if (administradores.esAdministradorInicial(usuario)) {
            roles.add(Rol.ADMIN);
            permisos.add(Permiso.VER_ROLES_AJENOS);
        }
        for (AsignacionRol a : repositorio.findByUsernameAdAndEstadoRegistro(usuario, EstadoRegistro.ACTIVO)) {
            roles.add(a.getRol());
            if (a.getRol() == Rol.ADMIN || a.puedeVerRolesAjenos()) {
                permisos.add(Permiso.VER_ROLES_AJENOS);
            }
        }
        return new RolesYPermisos(roles, permisos);
    }
}