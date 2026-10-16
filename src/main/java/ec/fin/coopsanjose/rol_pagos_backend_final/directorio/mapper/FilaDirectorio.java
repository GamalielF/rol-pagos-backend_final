package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.mapper;

import java.time.LocalDate;

/** Un usuario del AD ya normalizado, listo para compararse con la copia local. */
public record FilaDirectorio(String username, String cedula, String nombre, String cargo,
                             Integer secuenciaOficina, LocalDate fechaCreacion, boolean activo) {
}
