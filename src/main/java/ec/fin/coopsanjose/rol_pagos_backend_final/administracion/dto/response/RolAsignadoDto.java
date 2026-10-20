package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response;

/** origen: CONFIGURACION (administrador de arranque, intocable desde la API) o ASIGNADO. */
public record RolAsignadoDto(String rol, String origen, boolean verRolesAjenos) {
}
