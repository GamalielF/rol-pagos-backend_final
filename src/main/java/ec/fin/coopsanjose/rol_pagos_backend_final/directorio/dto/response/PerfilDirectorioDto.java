package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response;

/** Vista de un usuario del directorio. La cédula SIEMPRE sale enmascarada. */
public record PerfilDirectorioDto(String username, String nombreCompleto, String cargo, String email,
                                  String estado, String cedulaEnmascarada) {
}