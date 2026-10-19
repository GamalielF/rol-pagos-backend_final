package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

public record PublicacionDto(String periodo, String estadoPeriodo, LocalDateTime fechaPublicacion,
                             String publicadoPor, long rolesPublicados, long sinRol) {
}
