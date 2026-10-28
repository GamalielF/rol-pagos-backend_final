package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.NotificacionRolDto;

/**
 * Resultado de reenviar el aviso de un rol. "aceptado" significa que la API de correo aceptó el envío; no garantiza la
 * entrega. Si no se aceptó, el mensaje dice por qué y la notificación ya refleja el fallo.
 */
public record ResultadoReenvioDto(boolean aceptado, String mensaje, NotificacionRolDto notificacion) {
}
