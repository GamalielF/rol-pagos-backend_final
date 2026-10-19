package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.util.List;

public record ResumenPeriodoDto(String periodo, String estadoPeriodo, long rolesActivos, long rolesInactivos,
                                long personasActivas, long sinRol, List<String> nombresSinRol) {
}