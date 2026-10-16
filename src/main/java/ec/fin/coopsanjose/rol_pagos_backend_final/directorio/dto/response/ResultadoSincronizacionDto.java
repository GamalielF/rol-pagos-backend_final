package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response;

import java.time.LocalDateTime;

public record ResultadoSincronizacionDto(int recibidos, int creados, int actualizados, int sinCambios,
                                         int inactivados, int omitidos, int cedulasConDigitoInvalido,
                                         LocalDateTime inicio, LocalDateTime fin) {
}