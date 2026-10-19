package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;

import java.time.LocalDate;

public record FiltroAcciones(LocalDate desde, LocalDate hasta, String usuario, TipoAccion tipo, String periodo) {
}
