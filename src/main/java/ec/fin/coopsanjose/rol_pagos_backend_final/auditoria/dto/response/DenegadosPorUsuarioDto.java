package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response;

import java.time.LocalDateTime;

/** cedulasDistintas es el indicador de sondeo: muchas cédulas diferentes probadas por la misma cuenta. */
public record DenegadosPorUsuarioDto(String usuario, long intentos, long cedulasDistintas, LocalDateTime ultimoIntento) {
}