package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import java.util.Set;

/** Identidad del usuario de la petición. La construye el filtro de desarrollo hoy y el JWT después. */
public record UsuarioAutenticado(String username, String cedula, String nombre, Set<Rol> roles, Set<Permiso> permisos) {

    public UsuarioAutenticado {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        roles = (roles == null) ? Set.of() : Set.copyOf(roles);              // copia inmutable
        permisos = (permisos == null) ? Set.of() : Set.copyOf(permisos);
    }

    /** Constructor corto: RRHH y ADMIN reciben los permisos de siempre (así funciona la simulación de desarrollo). */
    public UsuarioAutenticado(String username, String cedula, String nombre, Set<Rol> roles) {
        this(username, cedula, nombre, roles, permisosPorDefecto(roles));
    }

    private static Set<Permiso> permisosPorDefecto(Set<Rol> roles) {
        boolean personal = roles != null && (roles.contains(Rol.RRHH) || roles.contains(Rol.ADMIN));
        return personal ? Set.of(Permiso.VER_ROLES_AJENOS) : Set.of();
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    public boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }
}