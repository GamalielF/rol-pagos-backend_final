package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.request;

/** Opcional. Para RRHH: false = puede cargar y publicar, pero no abrir los roles de otras personas. Por defecto true. */
public record OtorgarRolRequest(Boolean verRolesAjenos) {
}
