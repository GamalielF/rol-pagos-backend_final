package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request;

/** Si hay personas activas sin rol, RRHH debe confirmar de forma explícita. */
public record PublicarPeriodoRequest(Boolean confirmarFaltantes) {
}
