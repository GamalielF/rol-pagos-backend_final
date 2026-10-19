package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio al consultar el rol de otra persona: queda en la bitácora. */
public record MotivoConsultaRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 200, message = "El motivo debe tener entre 10 y 200 caracteres") String motivo) {
}
