package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response;

public record ResultadoEncoladoDto(long creadas, long pendientes, long omitidas, long yaExistentes) {
}
