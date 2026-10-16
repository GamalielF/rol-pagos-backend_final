package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

/** Resultado de UN archivo del lote. */
public record ResultadoArchivoDto(String archivo, EstadoCarga estado, String mensaje) {
}
