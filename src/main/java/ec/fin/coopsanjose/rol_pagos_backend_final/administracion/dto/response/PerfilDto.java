package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response;

import java.util.List;

/** Lo que el front necesita para armar los módulos. puedeConsultarSuRol = false si la cuenta no tiene cédula en el AD. */
public record PerfilDto(String username, String nombre, String cedulaEnmascarada, boolean puedeConsultarSuRol,
                        List<String> roles, List<String> permisos) {
}