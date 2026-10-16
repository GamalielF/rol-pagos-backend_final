package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

/** Datos de un usuario según el directorio del AD. */
public record PerfilDirectorio(String username, String cedula, String nombre, String email, boolean activo) {
}