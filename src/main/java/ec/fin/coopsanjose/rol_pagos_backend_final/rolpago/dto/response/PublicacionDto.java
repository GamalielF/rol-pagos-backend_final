package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

/** avisosEncolados = correos que saldrán en segundo plano (0 si los avisos están deshabilitados). */
public record PublicacionDto(String periodo, String estadoPeriodo, LocalDateTime fechaPublicacion,
                             String publicadoPor, long rolesPublicados, long sinRol, long rolesSinPersonaActiva,
                             long avisosEncolados) {
}
