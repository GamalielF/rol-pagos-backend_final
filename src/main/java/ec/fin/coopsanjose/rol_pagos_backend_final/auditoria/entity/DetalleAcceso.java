package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity;

/** Por qué se permitió o se denegó un acceso. */
public enum DetalleAcceso {
    OK,
    CEDULA_NO_COINCIDE,
    SIN_CEDULA_EN_DIRECTORIO,
    ROL_NO_ENCONTRADO,
    ARCHIVO_NO_DISPONIBLE,
    SIN_PERMISO
}
