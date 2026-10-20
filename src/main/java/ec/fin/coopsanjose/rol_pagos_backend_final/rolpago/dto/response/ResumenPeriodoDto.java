package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.util.List;

/**
 * El cuadre de un período, en los dos sentidos: personas activas sin rol (sinRol) y roles cuyo titular
 * ya no está activo en el AD (rolesSinPersonaActiva): esos nunca se leerán ni se avisarán.
 */
public record ResumenPeriodoDto(String periodo, String estadoPeriodo, long rolesActivos, long rolesInactivos,
                                long personasActivas, long sinRol, List<String> nombresSinRol,
                                long rolesSinPersonaActiva, List<String> nombresRolesSinPersona) {
}
