package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Motivo al retirar o reponer un rol. Antes de publicar no hace falta; en un período ya publicado es obligatorio
 * (el servicio lo exige, porque solo él sabe en qué estado está el período).
 */
public record MotivoCorreccionRequest(
        @Size(max = 500, message = "El motivo es demasiado largo") String motivo) {
}
