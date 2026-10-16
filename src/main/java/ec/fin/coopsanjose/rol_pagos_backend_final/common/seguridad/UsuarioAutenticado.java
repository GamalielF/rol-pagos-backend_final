package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import java.util.Set;

/** Identidad del usuario de la petición. La construye el filtro de desarrollo hoy y el JWT después. */
public record UsuarioAutenticado(String username, String cedula, String nombre, Set<Rol> roles) {

    public UsuarioAutenticado {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        roles = (roles == null) ? Set.of() : Set.copyOf(roles);   // copia inmutable
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }
}
