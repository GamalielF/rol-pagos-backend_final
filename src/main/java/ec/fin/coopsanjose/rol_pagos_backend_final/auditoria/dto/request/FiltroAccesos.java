package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;

import java.time.LocalDate;

/** Todos los campos son opcionales. Sin fechas: los últimos 30 días. */
public record FiltroAccesos(LocalDate desde, LocalDate hasta, String usuario, ResultadoAcceso resultado,
                            TipoAcceso tipo, String periodo) {
}
