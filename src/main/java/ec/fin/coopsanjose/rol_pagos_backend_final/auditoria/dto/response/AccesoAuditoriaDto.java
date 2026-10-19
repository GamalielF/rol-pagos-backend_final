package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response;

import java.time.LocalDateTime;

/** La cédula SIEMPRE sale enmascarada. */
public record AccesoAuditoriaDto(Long id, LocalDateTime fecha, String usuario, String tipoAcceso, String resultado,
                                 String detalle, String cedulaEnmascarada, String periodo, String motivo, String ip) {
}