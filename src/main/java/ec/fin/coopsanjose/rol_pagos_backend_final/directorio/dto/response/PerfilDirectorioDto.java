package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response;

/**
 * Vista de un usuario del directorio, solo para ADMIN. La cédula va completa, pero nunca en toString(), para que no
 * pueda pasar a los registros si alguien imprime el objeto.
 */
public record PerfilDirectorioDto(String username, String nombreCompleto, String cargo, String email,
                                  String estado, String cedula) {

    @Override
    public String toString() {
        return "PerfilDirectorioDto[username=" + username + ", estado=" + estado + "]";
    }
}
