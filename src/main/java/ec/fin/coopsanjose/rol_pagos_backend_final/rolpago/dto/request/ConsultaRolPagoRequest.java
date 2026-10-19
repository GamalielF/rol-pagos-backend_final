package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** El empleado teclea su cédula. El período es opcional: sin él se entrega el último publicado. */
public record ConsultaRolPagoRequest(
        @NotBlank(message = "La cédula es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos") String cedula,
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El período debe tener el formato yyyy-MM") String periodo) {
}
