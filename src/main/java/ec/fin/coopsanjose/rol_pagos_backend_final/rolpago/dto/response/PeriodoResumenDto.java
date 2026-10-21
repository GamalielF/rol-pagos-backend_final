package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

public record PeriodoResumenDto(String periodo, String estado, LocalDateTime fechaPublicacion,
                                String publicadoPor, long rolesActivos) {
}
