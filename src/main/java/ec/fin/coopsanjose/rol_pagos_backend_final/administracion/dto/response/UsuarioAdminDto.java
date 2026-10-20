package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response;

import java.util.List;

/** estadoDirectorio: ACTIVO, INACTIVO o NO_EXISTE. Sin cédula: no hace falta para administrar. */
public record UsuarioAdminDto(String username, String nombre, String cargo, String estadoDirectorio,
                              List<RolAsignadoDto> roles) {
}
