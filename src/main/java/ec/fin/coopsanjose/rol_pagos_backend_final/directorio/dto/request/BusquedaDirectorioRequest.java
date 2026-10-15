package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /admin/directorio/buscar. */
public record BusquedaDirectorioRequest(
        @NotBlank(message = "La identificación es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "La identificación debe tener 10 dígitos") String cedula) {
}