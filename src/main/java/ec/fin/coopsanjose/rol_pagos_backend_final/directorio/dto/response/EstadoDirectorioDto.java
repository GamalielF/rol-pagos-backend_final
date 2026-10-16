package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response;

import java.time.LocalDateTime;

public record EstadoDirectorioDto(long activos, long inactivos, LocalDateTime ultimaSincronizacion,
                                  String resultadoUltima, String mensajeUltima) {
}