package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

/** Una fila del listado de RRHH. La cédula SIEMPRE sale enmascarada. */
public record RolPagoResumenDto(Long id, String cedulaEnmascarada, String nombre, String estado,
                                long tamanoBytes, LocalDateTime actualizadoEn, String actualizadoPor) {
}
