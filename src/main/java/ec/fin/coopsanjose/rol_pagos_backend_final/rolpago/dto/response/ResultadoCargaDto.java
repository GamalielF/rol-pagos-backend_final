package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.util.List;

public record ResultadoCargaDto(String periodo, int total, int cargados, int reemplazados, int sinCambios,
                                int rechazados, List<ResultadoArchivoDto> resultados) {
}
