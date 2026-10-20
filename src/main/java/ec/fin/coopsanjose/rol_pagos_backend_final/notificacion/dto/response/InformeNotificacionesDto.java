package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response;

import java.util.List;

/** reintentando = avisos que ya fallaron alguna vez y esperan su siguiente intento. */
public record InformeNotificacionesDto(String periodo, boolean habilitadas, long total, long pendientes,
                                       long reintentando, long enviadas, long fallidas, long omitidas,
                                       List<ProblemaNotificacionDto> problemas) {
}
