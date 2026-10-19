package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response;

import java.time.LocalDateTime;

public record AccionAuditoriaDto(Long id, LocalDateTime fecha, String usuario, String tipoAccion, String periodo,
                                 Long rolPagoId, String detalle, String ip) {
}
