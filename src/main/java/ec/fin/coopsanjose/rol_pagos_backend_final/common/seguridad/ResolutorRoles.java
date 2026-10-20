package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import java.util.Set;

public interface ResolutorRoles {

    record RolesYPermisos(Set<Rol> roles, Set<Permiso> permisos) {
    }

    /** Siempre incluye EMPLEADO: todo usuario activo del AD lo es. */
    RolesYPermisos resolver(String username);
}