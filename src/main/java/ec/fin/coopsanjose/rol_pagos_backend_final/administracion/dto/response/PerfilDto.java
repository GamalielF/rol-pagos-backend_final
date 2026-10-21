package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response;

import java.util.List;

/** roles va ordenado del más al menos privilegiado (ADMIN, RRHH, EMPLEADO). */
public record PerfilDto(String username, String nombre, String cargo, String cedulaEnmascarada,
                        boolean puedeConsultarSuRol, List<String> roles, List<String> permisos) {
}