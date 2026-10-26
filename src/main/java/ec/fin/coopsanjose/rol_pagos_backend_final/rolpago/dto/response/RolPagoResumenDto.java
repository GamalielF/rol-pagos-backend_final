package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

/**
 * Una fila del listado de RRHH. Solo llegan aquí RRHH y ADMIN, por eso la cédula va completa; pero nunca aparece
 * en toString(), para que no pueda pasar a los registros si alguien imprime el objeto.
 */
public record RolPagoResumenDto(Long id, String cedula, String nombre, String estado,
                                long tamanoBytes, LocalDateTime actualizadoEn, String actualizadoPor) {

    @Override
    public String toString() {
        return "RolPagoResumenDto[id=" + id + ", estado=" + estado + ", tamanoBytes=" + tamanoBytes + "]";
    }
}
