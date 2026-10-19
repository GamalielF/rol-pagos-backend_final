package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.util.List;

public record ListadoPeriodoDto(String periodo, String estadoPeriodo, long activos, long inactivos,
                                List<RolPagoResumenDto> roles) {
}
