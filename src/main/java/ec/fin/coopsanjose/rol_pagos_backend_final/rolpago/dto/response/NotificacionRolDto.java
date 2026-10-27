package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import java.time.LocalDateTime;

/**
 * Cómo va el aviso por correo de un rol. estado: PENDIENTE (en cola o esperando un reintento), ENVIADA (aceptada por la
 * API de correo), FALLIDA (agotó los intentos o el fallo no tiene remedio) u OMITIDA (la persona no tiene cuenta con correo).
 * El error es una categoría del fallo, sin datos personales.
 */
public record NotificacionRolDto(String estado, int intentos, LocalDateTime enviadaEn, LocalDateTime proximoIntento,
                                 String ultimoError) {
}
