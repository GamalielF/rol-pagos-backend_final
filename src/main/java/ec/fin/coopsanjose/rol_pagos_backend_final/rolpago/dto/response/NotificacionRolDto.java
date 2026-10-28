package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;

import java.time.LocalDateTime;

/**
 * Cómo va el aviso por correo de un rol. estado: PENDIENTE (en cola o esperando un reintento), ENVIADA (aceptada por la
 * API de correo), FALLIDA (agotó los intentos o el fallo no tiene remedio) u OMITIDA (la persona no tiene cuenta con correo).
 * El error es una categoría del fallo, sin datos personales.
 */
public record NotificacionRolDto(String estado, int intentos, LocalDateTime enviadaEn, LocalDateTime proximoIntento,
                                 String ultimoError, LocalDateTime reenviadaEn, int reenvios) {

    public static NotificacionRolDto de(NotificacionRolPago aviso) {
        return aviso == null ? null
                : new NotificacionRolDto(aviso.getEstado().name(), aviso.getIntentos(), aviso.getEnviadaEn(),
                aviso.getProximoIntento(), aviso.getUltimoError(), aviso.getReenviadaEn(), aviso.getReenvios());
    }
}
